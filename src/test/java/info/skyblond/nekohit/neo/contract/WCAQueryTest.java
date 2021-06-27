package info.skyblond.nekohit.neo.contract;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.bouncycastle.util.Arrays;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

/**
 * This class test query methods for WCA. 
 * Including valid response and invalid or expection handle.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class WCAQueryTest extends ContractTestFramework  {
    private Wallet creatorWallet = getTestWallet();
    private Wallet testWallet = getTestWallet();

    @Test
    void testInvalidQueryWCA() {
        assertEquals(
            "", 
            assertDoesNotThrow(() -> {
                return ContractInvokeHelper.queryWCA(getWcaContract(), "some_invalid_id");
            })
        );
    }

    @Test
    void testValidQueryWCA() throws Throwable {
        var identifier = ContractInvokeHelper.createWCA(
            // stake: 1.00 * 1.00
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone"}, 
            new Long[] { System.currentTimeMillis() + 60*1000 }, 
            0, 100, "test_query_valid_wca_" + System.currentTimeMillis(), 
            testWallet
        );
        assertNotEquals(
            "", 
            assertDoesNotThrow(() -> {
                return ContractInvokeHelper.queryWCA(getWcaContract(), identifier);
            })
        );
    }

    @Test
    void testInvalidIdQueryPurchase() {
        assertEquals(
            0, 
            assertDoesNotThrow(() -> {
                return ContractInvokeHelper.queryPurchase(
                    getWcaContract(), "some_invalid_id", Account.create()
                ).longValue();
            })
        );
    }

    @Test
    void testInvalidBuyerQueryPurchase() throws Throwable {
        // create WCA
        var identifier = ContractInvokeHelper.createAndPayWCA(
            getWcaContract(), 1_00, 1_00, 
            new String[]{"milestone1", "milestone2", "milestone3"}, 
            new Long[] { 
                System.currentTimeMillis() + 60 * 1000,
                System.currentTimeMillis() + 61 * 1000,
                System.currentTimeMillis() + 62 * 1000
            }, 
            0, 1, "test_invalid_query_purchase_" + System.currentTimeMillis(), 
            creatorWallet
        );
        assertEquals(
            0, 
            assertDoesNotThrow(() -> {
                return ContractInvokeHelper.queryPurchase(
                    getWcaContract(), identifier, Account.create()
                ).longValue();
            })
        );
    }

    @Test
    void testValidBuyerQueryPurchase() throws Throwable {
        var purchaseAmount = 1000_00;
        // create WCA
        var identifier = ContractInvokeHelper.createAndPayWCA(
            getWcaContract(), 1_00, purchaseAmount, 
            new String[]{"milestone1"}, 
            new Long[] { 
                System.currentTimeMillis() + 60 * 1000
            }, 
            0, 1, "test_valid_query_purchase_" + System.currentTimeMillis(), 
            creatorWallet
        );

        transferToken(
            getCatToken(), testWallet,
            getWcaContractAddress(), 
            purchaseAmount, identifier, true
        );

        assertEquals(
            purchaseAmount, 
            assertDoesNotThrow(() -> {
                return ContractInvokeHelper.queryPurchase(
                    getWcaContract(), identifier, testWallet.getDefaultAccount()
                ).longValue();
            })
        );
    }

    @Test 
    void testOwnerHash() throws Exception {
        var actualOwnerHexString = new Hash160(Arrays.reverse(Hex.decodeHex(
            testInvoke(getCatToken(), "contractOwner", new ContractParameter[0], new Signer[0]).getStack().get(0).getHexString()
        )));
        
        assertEquals(CONTRACT_OWNER_WALLET.getDefaultAccount().getScriptHash(), actualOwnerHexString);
    }
}
