package info.skyblond.nekohit.neo.contract;

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
                assertDoesNotThrow(() -> {
                    return ContractInvokeHelper.queryWCA(getWcaContract(), "some_invalid_id");
                })
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
                assertDoesNotThrow(() -> {
                    return ContractInvokeHelper.queryPurchase(
                            getWcaContract(), identifier, this.testWallet.getDefaultAccount()
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

    @Test
    void testLargeAdvancedQuery() throws Throwable {
        // Skip this test if is on public chain
        if (isPublicChain())
            return;

        var buyerWallet = getTestWallet();
        var msCount = 10;
        var milestoneTitle = new String[msCount];
        var milestoneDescriptions = new String[msCount];
        var milestoneEndTime = new Long[msCount];

        for (int i = 0; i < msCount; i++) {
            milestoneTitle[i] = "Title ".repeat(100);
            milestoneDescriptions[i] = "description ".repeat(100);
            milestoneEndTime[i] = System.currentTimeMillis() * 2 + i;
        }

        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            Wallet finalBuyerWallet = buyerWallet;
            new Thread(() -> {
                try {
                    ContractInvokeHelper.createWCA(
                            getWcaContract(), "description ".repeat(100),
                            1_00, 1_00,
                            milestoneTitle, milestoneDescriptions, milestoneEndTime,
                            0, 1, true,
                            "test_large_query_" + finalI + "_" + System.currentTimeMillis(),
                            finalBuyerWallet
                    );
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }).start();
            if (i % 20 == 0) {
                buyerWallet = getTestWallet();
            }
        }

        Thread.sleep(5000);
        System.out.println(assertDoesNotThrow(() -> ContractInvokeHelper.advanceQuery(
                getWcaContract(),
                Hash160.ZERO, Hash160.ZERO, 1, 9999
        )));
    }
}
