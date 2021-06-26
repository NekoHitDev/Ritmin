package info.skyblond.nekohit.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Wallet;

/**
 * Test the CatToken.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class CatTokenTest extends ContractTestFramework {
    private Wallet testWallet = getTestWallet();

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
                        Signer.calledByEntry(CONTRACT_OWNER_WALLET.getDefaultAccount())
                    },
                    CONTRACT_OWNER_WALLET
                );
            }
        );
    }

    @Test
    void testNotOwner() {
        var throwable = assertThrows(
            TransactionConfigurationException.class,
            () -> invokeFunction(
                getCatToken(),
                "verify",
                new ContractParameter[0],
                new Signer[]{
                    Signer.calledByEntry(testWallet.getDefaultAccount())
                },
                testWallet
            )
        );
        assertTrue(
            throwable.getMessage().contains("The calling entity is not the owner of this contract."),
            "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidAmount() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class,
            () -> invokeFunction(
                getCatToken(), "transfer", 
                new ContractParameter[] {
                    ContractParameter.hash160(testWallet.getDefaultAccount()),
                    ContractParameter.hash160(testWallet.getDefaultAccount()),
                    ContractParameter.integer(-100),
                    ContractParameter.any(null)
                },
                new Signer[] {Signer.calledByEntry(testWallet.getDefaultAccount())},
                testWallet
            )
        );
        assertTrue(
            throwable.getMessage().contains("The transfer amount was negative."), 
            "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidSigner() throws Throwable {
        var tempWallet = Wallet.create();
        var throwable = assertThrows(
            TransactionConfigurationException.class,
            () -> invokeFunction(
                getCatToken(), "transfer", 
                new ContractParameter[] {
                    ContractParameter.hash160(tempWallet.getDefaultAccount()),
                    ContractParameter.hash160(Hash160.ZERO),
                    ContractParameter.integer(100),
                    ContractParameter.any(null)
                },
                new Signer[] {Signer.calledByEntry(testWallet.getDefaultAccount())},
                testWallet
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
        var oldFromBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();
        var oldToBalance = getCatToken().getBalanceOf(toWallet.getDefaultAccount()).longValue();

        // do the transfer
        assertDoesNotThrow(
            () -> transferToken(
                getCatToken(), testWallet, 
                toWallet.getDefaultAccount().getScriptHash(), 
                transferAmount, null, true
            )
        );

        // query new balance
        var newFromBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();
        var newToBalance = getCatToken().getBalanceOf(toWallet.getDefaultAccount()).longValue();

        // check change
        assertEquals(transferAmount, oldFromBalance - newFromBalance);
        assertEquals(transferAmount, newToBalance - oldToBalance);
    }
}
