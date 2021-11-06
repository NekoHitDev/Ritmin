package com.nekohit.neo.contract;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the CatToken.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class CatTokenTest extends ContractTestFramework {
    private final Wallet testWallet = getTestWallet();

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
        assertTrue(invokeFunction(
                        getCatToken(),
                        "verify",
                        new ContractParameter[0],
                        new Signer[]{
                                AccountSigner.calledByEntry(CONTRACT_OWNER_WALLET.getDefaultAccount())
                        },
                        CONTRACT_OWNER_WALLET
                ).getExecutions().get(0).getStack().get(0).getBoolean()
        );
    }

    @Test
    void testNotOwner() {
        assertThrows(TransactionConfigurationException.class, () -> invokeFunction(
                getCatToken(),
                "verify",
                new ContractParameter[0],
                new Signer[]{
                        AccountSigner.calledByEntry(this.testWallet.getDefaultAccount())
                },
                this.testWallet
        ));
    }

    @Test
    void testInvalidAmount() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> invokeFunction(
                        getCatToken(), "transfer",
                        new ContractParameter[]{
                                ContractParameter.hash160(this.testWallet.getDefaultAccount()),
                                ContractParameter.hash160(this.testWallet.getDefaultAccount()),
                                ContractParameter.integer(-100),
                                ContractParameter.any(null)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testWallet.getDefaultAccount())},
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains("The transfer amount was negative."),
                "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidSigner() {
        var tempWallet = Wallet.create();
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> invokeFunction(
                        getCatToken(), "transfer",
                        new ContractParameter[]{
                                ContractParameter.hash160(tempWallet.getDefaultAccount()),
                                ContractParameter.hash160(Hash160.ZERO),
                                ContractParameter.integer(100),
                                ContractParameter.any(null)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testWallet.getDefaultAccount())},
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains("Invalid sender signature."),
                "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testNormalTransfer() throws Throwable {
        var toWallet = Wallet.create();
        var transferAmount = 1000_00;
        // Query old balance
        var oldFromBalance = getCatToken().getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        var oldToBalance = getCatToken().getBalanceOf(toWallet.getDefaultAccount()).longValue();

        // do the transfer
        assertDoesNotThrow(
                () -> transferToken(
                        getCatToken(), this.testWallet,
                        toWallet.getDefaultAccount().getScriptHash(),
                        transferAmount, null, true
                )
        );

        // query new balance
        var newFromBalance = getCatToken().getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        var newToBalance = getCatToken().getBalanceOf(toWallet.getDefaultAccount()).longValue();

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
                                ContractParameter.hash160(this.testWallet.getDefaultAccount()),
                                ContractParameter.integer(100),
                                ContractParameter.any(null)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testWallet.getDefaultAccount())},
                        this.testWallet
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
                                ContractParameter.hash160(this.testWallet.getDefaultAccount()),
                                ContractParameter.integer(Integer.MAX_VALUE)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testWallet.getDefaultAccount())},
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains("Insufficient amount."),
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
                        new Signer[]{AccountSigner.calledByEntry(this.testWallet.getDefaultAccount())},
                        this.testWallet
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
                                ContractParameter.hash160(this.testWallet.getDefaultAccount()),
                                ContractParameter.integer(-100)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testWallet.getDefaultAccount())},
                        this.testWallet
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
                        GAS_TOKEN, this.testWallet, getCatTokenAddress(),
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
        var exchangeAmount = 1_0000_0000L;
        // Query old balance
        var oldCatBalance = getCatToken().getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        var oldTotalSupply = new FungibleToken(getCatTokenAddress(), NEOW3J).getTotalSupply().longValue();

        // do the transfer
        assertDoesNotThrow(
                () -> transferToken(
                        GAS_TOKEN, this.testWallet, getCatTokenAddress(),
                        exchangeAmount, null, true
                )
        );

        // query new balance
        var newCatBalance = getCatToken().getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        var newTotalSupply = new FungibleToken(getCatTokenAddress(), NEOW3J).getTotalSupply().longValue();

        // check change
        assertEquals(2_00, newCatBalance - oldCatBalance);
        assertEquals(2_00, newTotalSupply - oldTotalSupply);
    }

    @Test
    void testNormalDestroy() throws IOException {
        var destroyAmount = 2_00;
        // Query old balance
        var oldCatBalance = getCatToken().getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        var oldGasBalance = GAS_TOKEN.getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        var oldTotalSupply = new FungibleToken(getCatTokenAddress(), NEOW3J).getTotalSupply().longValue();

        // do the transfer
        assertDoesNotThrow(
                () -> invokeFunction(
                        getCatToken(), "destroyToken",
                        new ContractParameter[]{
                                ContractParameter.hash160(this.testWallet.getDefaultAccount()),
                                ContractParameter.integer(destroyAmount)
                        },
                        new Signer[]{AccountSigner.calledByEntry(this.testWallet.getDefaultAccount())},
                        this.testWallet
                )
        );

        // query new balance
        var newCatBalance = getCatToken().getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        var newGasBalance = GAS_TOKEN.getBalanceOf(this.testWallet.getDefaultAccount()).longValue();
        var newTotalSupply = new FungibleToken(getCatTokenAddress(), NEOW3J).getTotalSupply().longValue();

        // check change
        assertEquals(2_00, oldCatBalance - newCatBalance);
        assertEquals(2_00, oldTotalSupply - newTotalSupply);
        // need to account the gas fee
        assertEquals(1_0000_0000L - 2123_1780L, newGasBalance - oldGasBalance);
    }
}
