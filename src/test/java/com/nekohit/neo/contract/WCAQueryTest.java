package com.nekohit.neo.contract;

import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test query methods for WCA.
 * Including valid response and invalid or expection handle.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class WCAQueryTest extends ContractTestFramework {
    private final Wallet creatorWallet = getTestWallet();
    private final Wallet testWallet = getTestWallet();

    @Test
    void testInvalidQueryWCA() {
        assertEquals(
                "",
                assertDoesNotThrow(() -> ContractInvokeHelper.queryWCA(getWcaContract(), "some_invalid_id"))
        );
    }

    @Test
    void testValidQueryWCA() throws Throwable {
        var identifier = ContractInvokeHelper.createWCA(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                "test_query_valid_wca_" + System.currentTimeMillis(),
                this.testWallet
        );
        assertNotEquals(
                "",
                assertDoesNotThrow(() -> ContractInvokeHelper.queryWCA(getWcaContract(), identifier))
        );
    }

    @Test
    void testInvalidIdQueryPurchase() {
        assertEquals(
                0,
                assertDoesNotThrow(() -> ContractInvokeHelper.queryPurchase(
                        getWcaContract(), "some_invalid_id", Account.create()
                ).longValue())
        );
    }

    @Test
    void testInvalidBuyerQueryPurchase() throws Throwable {
        // create WCA
        var identifier = ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, false,
                "test_invalid_query_purchase_" + System.currentTimeMillis(),
                this.creatorWallet
        );
        assertEquals(
                0,
                assertDoesNotThrow(() -> ContractInvokeHelper.queryPurchase(
                        getWcaContract(), identifier, Account.create()
                ).longValue())
        );
    }

    @Test
    void testValidBuyerQueryPurchase() throws Throwable {
        var purchaseAmount = 1000_00;
        // create WCA
        var identifier = ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, purchaseAmount,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000
                },
                0, 1, false,
                "test_valid_query_purchase_" + System.currentTimeMillis(),
                this.creatorWallet
        );

        transferToken(
                getCatToken(), this.testWallet,
                getWcaContractAddress(),
                purchaseAmount, identifier, true
        );

        assertEquals(
                purchaseAmount,
                assertDoesNotThrow(() -> ContractInvokeHelper.queryPurchase(
                        getWcaContract(), identifier, this.testWallet.getDefaultAccount()
                ).longValue())
        );
    }

    @Test
    void testOwnerHash() throws Exception {
        var actualOwnerHexString = new Hash160(Arrays.reverse(Hex.decodeHex(
                testInvoke(getCatToken(), "contractOwner", new ContractParameter[0], new Signer[0]).getStack().get(0).getHexString()
        )));

        assertEquals(CONTRACT_OWNER_WALLET.getDefaultAccount().getScriptHash(), actualOwnerHexString);
    }

    @Test
    void testAdvancedQuery() throws Throwable {
        var buyerWallet = getTestWallet();
        var unpaidWCA = ContractInvokeHelper.createWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, false,
                "test_advanced_unpaid_" + System.currentTimeMillis(),
                this.creatorWallet
        );
        var canPurchaseWCA = ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 2_00,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, false,
                "test_advanced_can_buy_" + System.currentTimeMillis(),
                this.creatorWallet
        );
        transferToken(getCatToken(), buyerWallet, getWcaContractAddress(), 1_00, canPurchaseWCA, false);
        var onGoingWCA = ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, false,
                "test_advanced_going_" + System.currentTimeMillis(),
                this.creatorWallet
        );
        ContractInvokeHelper.finishMilestone(getWcaContract(), onGoingWCA, 0, "123", this.creatorWallet);
        var finishedWCA = ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000
                },
                0, 1, false,
                "test_advanced_finished_" + System.currentTimeMillis(),
                this.creatorWallet
        );
        ContractInvokeHelper.finishMilestone(getWcaContract(), finishedWCA, 0, "123", this.creatorWallet);

        // TODO toggle those to test
        System.out.println(assertDoesNotThrow(() -> ContractInvokeHelper.advanceQuery(
                getWcaContract(),
                this.creatorWallet.getDefaultAccount().getScriptHash(),
                Hash160.ZERO, 1, 20
        )));
    }
}
