package info.skyblond.nekohit.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;

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
@ExtendWith(ContractTestFramework.class)
public class CatTokenTest extends ContractTestFramework {

    private static final long TRANSFER_AMOUNT = 1000_00;

    private Wallet testWallet = Wallet.create();

    @BeforeAll
    void prepareTestAccount() throws Throwable {
        prepareGas(testWallet.getDefaultAccount().getScriptHash(), 10_00000000, true);
    }

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
    void testOwnerHash() throws Exception {
        var actualOwnerHexString = new Hash160(Arrays.reverse(Hex.decodeHex(
            testInvoke(getCatToken(), "contractOwner", new ContractParameter[0], new Signer[0]).getStack().get(0).getHexString()
        )));
        
        assertEquals(CONTRACT_OWNER_WALLET.getDefaultAccount().getScriptHash(), actualOwnerHexString);
    }

    @Test
    void testInvalidAmount() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class,
            () -> invokeFunction(
                getCatToken(), "transfer", 
                new ContractParameter[] {
                    ContractParameter.hash160(CONTRACT_OWNER_WALLET.getDefaultAccount()),
                    ContractParameter.hash160(testWallet.getDefaultAccount()),
                    ContractParameter.integer(-100),
                    ContractParameter.any(null)
                },
                new Signer[] {Signer.calledByEntry(CONTRACT_OWNER_WALLET.getDefaultAccount())},
                CONTRACT_OWNER_WALLET
            )
        );
        assertTrue(
            throwable.getMessage().contains("The transfer amount was negative."), 
            "Unexpected message: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidSigner() throws Throwable {
        var throwable = assertThrows(
            TransactionConfigurationException.class,
            () -> invokeFunction(
                getCatToken(), "transfer", 
                new ContractParameter[] {
                    ContractParameter.hash160(CONTRACT_OWNER_WALLET.getDefaultAccount()),
                    ContractParameter.hash160(testWallet.getDefaultAccount()),
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
        // Query old balance
        var oldFromBalance = getCatToken().getBalanceOf(CONTRACT_OWNER_WALLET.getDefaultAccount()).longValue();
        var oldToBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();

        // do the transfer
        assertDoesNotThrow(
            () -> transferToken(
                getCatToken(), CONTRACT_OWNER_WALLET, 
                testWallet.getDefaultAccount().getScriptHash(), 
                TRANSFER_AMOUNT, null, true
            )
        );

        // query new balance
        var newFromBalance = getCatToken().getBalanceOf(CONTRACT_OWNER_WALLET.getDefaultAccount()).longValue();
        var newToBalance = getCatToken().getBalanceOf(testWallet.getDefaultAccount()).longValue();

        // check change
        assertEquals(TRANSFER_AMOUNT, oldFromBalance - newFromBalance);
        assertEquals(TRANSFER_AMOUNT, newToBalance - oldToBalance);
    }
}
