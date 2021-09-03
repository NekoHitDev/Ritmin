package com.nekohit.neo.contract;

import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
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
    void testTotalSupply() throws UnexpectedReturnTypeException, IOException {
        assertEquals(getCatToken().getTotalSupply().longValue(), 1_000_000_000_00L);
    }

    @Test
    void testDecimal() throws UnexpectedReturnTypeException, IOException {
        assertEquals(getCatToken().getDecimals(), 2);
    }

    @Test
    void testIsOwner() {
        assertDoesNotThrow(
                () -> {
                    invokeFunction(
                            getCatToken(),
                            "verify",
                            new ContractParameter[0],
                            new Signer[]{
                                    AccountSigner.calledByEntry(CONTRACT_OWNER_WALLET.getDefaultAccount())
                            },
                            CONTRACT_OWNER_WALLET
                    );
                }
        );
    }

    @Test
    void testNotOwner() throws Throwable {
        assertFalse(invokeFunction(
                getCatToken(),
                "verify",
                new ContractParameter[0],
                new Signer[]{
                        AccountSigner.calledByEntry(this.testWallet.getDefaultAccount())
                },
                this.testWallet
        ).getExecutions().get(0).getStack().get(0).getBoolean());
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
}
