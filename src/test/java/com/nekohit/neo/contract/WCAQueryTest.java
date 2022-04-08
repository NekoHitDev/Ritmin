package com.nekohit.neo.contract;

import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static com.nekohit.neo.contract.ContractInvokeHelper.queryProject;
import static com.nekohit.neo.contract.ContractInvokeHelper.queryPurchase;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test query methods for WCA.
 * Including valid response and invalid or exception handle.
 */
@ContractTest(blockTime = 1, contracts = {
        CatToken.class,
        WCAContract.class,
})
public class WCAQueryTest extends ContractTestFramework {
    private Account creatorAccount;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        creatorAccount = getTestAccount();
        testAccount = getTestAccount();
    }

    @Test
    void testListPurchaseRecord() throws Throwable {
        // create WCA
        var identifier = ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1000_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000
                },
                0, 1, false,
                "test_dump_pr_" + System.currentTimeMillis(),
                this.creatorAccount
        );
        // purchase
        transferToken(
                getCatToken(), this.testAccount,
                getWcaContractAddress(),
                1000_00, identifier, true
        );
        // query
        List<StackItem> result = testInvoke(
                getWcaContract(),
                "listPurchaseRecord",
                new ContractParameter[]{
                        ContractParameter.string(identifier),
                        ContractParameter.integer(1),
                        ContractParameter.integer(20)
                },
                new Signer[]{}
        ).getStack().get(0).getList();
        for (StackItem elem : result) {
            List<StackItem> pair = elem.getList();
            String address = pair.get(0).getAddress();
            BigInteger value = pair.get(1).getInteger();
            System.out.println(address + ": " + value);

            BigInteger expectedValue = queryPurchase(
                    getWcaContract(), identifier, Account.fromAddress(address)
            );
            assertEquals(expectedValue, value);
        }
    }

    @Test
    void testInvalidQueryWCA() {
        assertEquals(
                "",
                assertDoesNotThrow(() -> queryProject(getWcaContract(), "some_invalid_id"))
        );
    }

    @Test
    void testValidQueryWCA() throws Throwable {
        var identifier = ContractInvokeHelper.declareProject(
                // stake: 1.00 * 1.00
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                "test_query_valid_wca_" + System.currentTimeMillis(),
                this.testAccount
        );
        assertNotEquals(
                "",
                assertDoesNotThrow(() -> queryProject(getWcaContract(), identifier))
        );
    }

    @Test
    void testInvalidIdQueryPurchase() {
        assertEquals(
                0,
                assertDoesNotThrow(() -> queryPurchase(
                        getWcaContract(), "some_invalid_id", Account.create()
                ).longValue())
        );
    }

    @Test
    void testInvalidBuyerQueryPurchase() throws Throwable {
        // create WCA
        var identifier = ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, false,
                "test_invalid_query_purchase_" + System.currentTimeMillis(),
                this.creatorAccount
        );
        assertEquals(
                0,
                assertDoesNotThrow(() -> queryPurchase(
                        getWcaContract(), identifier, Account.create()
                ).longValue())
        );
    }

    @Test
    void testValidBuyerQueryPurchase() throws Throwable {
        var purchaseAmount = 1000_00;
        // create WCA
        var identifier = ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, purchaseAmount,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000
                },
                0, 1, false,
                "test_valid_query_purchase_" + System.currentTimeMillis(),
                this.creatorAccount
        );

        transferToken(
                getCatToken(), this.testAccount,
                getWcaContractAddress(),
                purchaseAmount, identifier, true
        );

        assertEquals(
                purchaseAmount,
                assertDoesNotThrow(() -> queryPurchase(
                        getWcaContract(), identifier, this.testAccount
                ).longValue())
        );
    }

    @Test
    void testOwnerHash() throws Exception {
        var actualOwnerHexString = new Hash160(Arrays.reverse(Hex.decodeHex(
                testInvoke(getCatToken(), "contractOwner", new ContractParameter[0], new Signer[0]).getStack().get(0).getHexString()
        )));

        assertEquals(CONTRACT_OWNER_ACCOUNT.getScriptHash(), actualOwnerHexString);
    }

    @Test
    void testIsOwner() throws Throwable {
        assertTrue(invokeFunction(
                        getWcaContract(),
                        "verify",
                        new ContractParameter[0],
                        new Signer[]{
                                AccountSigner.calledByEntry(CONTRACT_OWNER_ACCOUNT)
                        }
                ).getExecutions().get(0).getStack().get(0).getBoolean()
        );
    }

    @Test
    void testAdvancedQuery() throws Throwable {
        var buyerAccount = getTestAccount();
        ContractInvokeHelper.declareProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, true,
                "test_advanced_unpaid_" + System.currentTimeMillis(),
                this.creatorAccount
        );
        var canPurchaseWCA = ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 2_00,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, true,
                "test_advanced_can_buy_" + System.currentTimeMillis(),
                this.creatorAccount
        );
        transferToken(getCatToken(), buyerAccount, getWcaContractAddress(), 1_00, canPurchaseWCA, false);
        var onGoingWCA = ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, true,
                "test_advanced_going_" + System.currentTimeMillis(),
                this.creatorAccount
        );
        ContractInvokeHelper.finishMilestone(getWcaContract(), onGoingWCA, 0, "123", this.creatorAccount);
        var finishedWCA = ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000
                },
                0, 1, true,
                "test_advanced_finished_" + System.currentTimeMillis(),
                this.creatorAccount
        );
        ContractInvokeHelper.finishMilestone(getWcaContract(), finishedWCA, 0, "123", this.creatorAccount);

        System.out.println(assertDoesNotThrow(() -> ContractInvokeHelper.advanceQuery(
                getWcaContract(), getCatTokenAddress(),
                this.creatorAccount.getScriptHash(),
                Hash160.ZERO, 1, 20
        )));
        System.out.println(assertDoesNotThrow(() -> ContractInvokeHelper.advanceQuery(
                getWcaContract(), getCatTokenAddress(),
                Hash160.ZERO,
                buyerAccount.getScriptHash(), 1, 20
        )));
        System.out.println(assertDoesNotThrow(() -> ContractInvokeHelper.advanceQuery(
                getWcaContract(), getCatTokenAddress(),
                Hash160.ZERO, Hash160.ZERO, 1, 20
        )));
    }
}
