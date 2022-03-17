package com.nekohit.neo.contract;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.test.ContractTest;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the CatToken.
 */
@ContractTest(blockTime = 1, contracts = {
        CatToken.class,
})
public class CatTokenTest extends ContractTestFramework {
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = getTestAccount();
    }

    @Test
    void testSymbol() throws UnexpectedReturnTypeException, IOException {
        assertEquals(getCatToken().getSymbol(), "CAT");
    }

    @Test
    void testDecimal() throws UnexpectedReturnTypeException, IOException {
        assertEquals(getCatToken().getDecimals(), 2);
    }

    @Test
    void testIsOwner() throws Throwable {
        assertTrue(testInvoke(
                        getCatToken(),
                        "verify",
                        new ContractParameter[0],
                        new Signer[]{
                                AccountSigner.calledByEntry(CONTRACT_OWNER_ACCOUNT)
                        }
                ).getStack().get(0).getBoolean()
        );
    }

    @Test
    void testNotOwner() {
        assertThrows(TransactionConfigurationException.class, () -> invokeFunction(
                getCatToken(),
                "verify",
                new ContractParameter[0],
                new Signer[]{
                        AccountSigner.calledByEntry(this.testAccount)
                }
        ));
    }

    @Test
    void testInvalidAmount() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> invokeFunction(
                        getCatToken(), "transfer",
                        new ContractParameter[]{
                                ContractParameter.hash160(this.testAccount),
                                ContractParameter.hash160(this.testAccount),
                                ContractParameter.integer(-100),
                                ContractParameter.any(null)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testAccount)}
                )
        );
        assertTrue(
                throwable.getMessage().contains("The transfer amount was negative."),
                "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidSigner() {
        var tempAccount = Account.create();
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> invokeFunction(
                        getCatToken(), "transfer",
                        new ContractParameter[]{
                                ContractParameter.hash160(tempAccount),
                                ContractParameter.hash160(Hash160.ZERO),
                                ContractParameter.integer(100),
                                ContractParameter.any(null)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testAccount)}
                )
        );
        assertTrue(
                throwable.getMessage().contains("Invalid sender signature."),
                "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testNormalTransfer() throws Throwable {
        var toAccount = Account.create();
        var transferAmount = 1000_00;
        // Query old balance
        var oldFromBalance = getCatToken().getBalanceOf(this.testAccount).longValue();
        var oldToBalance = getCatToken().getBalanceOf(toAccount).longValue();

        // do the transfer
        assertDoesNotThrow(
                () -> transferToken(
                        getCatToken(), this.testAccount,
                        toAccount.getScriptHash(),
                        transferAmount, null, true
                )
        );

        // query new balance
        var newFromBalance = getCatToken().getBalanceOf(this.testAccount).longValue();
        var newToBalance = getCatToken().getBalanceOf(toAccount).longValue();

        // check change
        assertEquals(transferAmount, oldFromBalance - newFromBalance);
        assertEquals(transferAmount, newToBalance - oldToBalance);
    }

    @Test
    void testIncorrectCaller() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> invokeFunction(
                        getCatToken(), "onNEP17Payment",
                        new ContractParameter[]{
                                ContractParameter.hash160(this.testAccount),
                                ContractParameter.integer(100),
                                ContractParameter.any(null)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testAccount)}
                )
        );
        assertTrue(
                throwable.getMessage().contains("Invalid caller."),
                "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testInsufficientDestroy() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> invokeFunction(
                        getCatToken(), "destroyToken",
                        new ContractParameter[]{
                                ContractParameter.hash160(this.testAccount),
                                ContractParameter.integer(Integer.MAX_VALUE)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testAccount)}
                )
        );
        assertTrue(
                throwable.getMessage().contains("Insufficient balance."),
                "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testDestroyWrongSign() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> invokeFunction(
                        getCatToken(), "destroyToken",
                        new ContractParameter[]{
                                ContractParameter.hash160(Account.create()),
                                ContractParameter.integer(Integer.MAX_VALUE)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testAccount)}
                )
        );
        assertTrue(
                throwable.getMessage().contains("Invalid sender signature."),
                "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testDestroyNegativeAmount() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> invokeFunction(
                        getCatToken(), "destroyToken",
                        new ContractParameter[]{
                                ContractParameter.hash160(this.testAccount),
                                ContractParameter.integer(-100)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testAccount)}
                )
        );
        assertTrue(
                throwable.getMessage().contains("The destroy amount was negative."),
                "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testMintNonexchangeableAmount() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> transferToken(
                        gasToken, this.testAccount, getCatTokenAddress(),
                        1, null, false
                )
        );
        assertTrue(
                throwable.getMessage().contains("Nonexchangeable amount detected."),
                "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testNormalExchange() throws IOException {
        var exchangeAmount = 1_000000L;
        // Query old balance
        var oldCatBalance = getCatToken().getBalanceOf(this.testAccount).longValue();
        var oldTotalSupply = new FungibleToken(getCatTokenAddress(), neow3j).getTotalSupply().longValue();

        // do the transfer
        assertDoesNotThrow(
                () -> transferToken(
                        gasToken, this.testAccount, getCatTokenAddress(),
                        exchangeAmount, null, true
                )
        );

        // query new balance
        var newCatBalance = getCatToken().getBalanceOf(this.testAccount).longValue();
        var newTotalSupply = new FungibleToken(getCatTokenAddress(), neow3j).getTotalSupply().longValue();

        // check change
        assertEquals(2_00, newCatBalance - oldCatBalance);
        assertEquals(2_00, newTotalSupply - oldTotalSupply);
    }

    @Test
    void testNormalDestroy() throws IOException {
        var destroyAmount = 2_00;
        // Query old balance
        var oldCatBalance = getCatToken().getBalanceOf(this.testAccount).longValue();
        var oldGasBalance = gasToken.getBalanceOf(this.testAccount).longValue();
        var oldTotalSupply = new FungibleToken(getCatTokenAddress(), neow3j).getTotalSupply().longValue();

        // do the transfer
        assertDoesNotThrow(
                () -> invokeFunction(
                        getCatToken(), "destroyToken",
                        new ContractParameter[]{
                                ContractParameter.hash160(this.testAccount),
                                ContractParameter.integer(destroyAmount)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testAccount)}
                )
        );

        // query new balance
        var newCatBalance = getCatToken().getBalanceOf(this.testAccount).longValue();
        var newGasBalance = gasToken.getBalanceOf(this.testAccount).longValue();
        var newTotalSupply = new FungibleToken(getCatTokenAddress(), neow3j).getTotalSupply().longValue();

        // check change
        assertEquals(2_00, oldCatBalance - newCatBalance);
        assertEquals(2_00, oldTotalSupply - newTotalSupply);
        // need to account the gas fee
        assertEquals(1_000000L - 1923_8860L, newGasBalance - oldGasBalance);
    }
}
