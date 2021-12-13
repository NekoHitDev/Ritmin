package com.nekohit.neo.contract;

import com.nekohit.neo.domain.ExceptionMessages;
import io.neow3j.test.ContractTest;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.wallet.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test the finish wca method for WCA.
 * Including invalid id, unpaid, not ready to finish(last ms not finished nor expired),
 * owner override, double finished, normal op(check token distribution)
 */
@ContractTest(blockTime = 1, contracts = {
        CatToken.class,
        WCAContract.class,
})
public class WCAFinishWCATest extends ContractTestFramework {
    private Account creatorAccount;
    private Account buyerAccount1;
    private Account buyerAccount2;

    @BeforeEach
    void setUp() {
        creatorAccount = getTestAccount();
        buyerAccount1 = getTestAccount();
        buyerAccount2 = getTestAccount();
    }

    @Test
    void testInvalidId() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishProject(
                        getWcaContract(), "some_invalid_id", this.creatorAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.RECORD_NOT_FOUND),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishUnpaid() throws Throwable {
        var identifier = "test_finish_unpaid_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.declareProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishProject(
                        getWcaContract(), identifier, this.creatorAccount
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNotFinished() throws Throwable {
        var identifier = "test_finish_not_ready_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishProject(
                        getWcaContract(), identifier, this.buyerAccount1
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STAGE_ALLOW_READY_TO_FINISH),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testOwnerOverride() throws Throwable {
        var identifier = "test_finish_owner_override_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.finishProject(
                        getWcaContract(), identifier, this.creatorAccount
                )
        );
    }

    @Test
    void testDoubleFinish() throws Throwable {
        var identifier = "test_double_finish_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        ContractInvokeHelper.finishProject(
                getWcaContract(), identifier, this.creatorAccount
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishProject(
                        getWcaContract(), identifier, this.buyerAccount1
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishFinished() throws Throwable {
        var identifier = "test_finish_finished_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "something", this.creatorAccount
        );
        // By finish the last ms, WCA is finished.
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishProject(
                        getWcaContract(), identifier, this.buyerAccount1
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STATUS_ALLOW_ONGOING),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishExpired() throws Throwable {
        var identifier = "test_finish_expired_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), 1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 2 * 1000},
                0, 100, false,
                identifier, this.creatorAccount
        );

        // let the last one expire
        Thread.sleep(2 * 1000);

        assertDoesNotThrow(
                () -> ContractInvokeHelper.finishProject(
                        getWcaContract(), identifier, this.buyerAccount1
                )
        );
    }

    @Test
    void testTokenDistribution() throws Throwable {
        var buyer1Purchase = 400_00;
        var buyer2Purchase = 500_00;
        var totalAmount = 1000_00;
        var stakeRate = 10;
        var identifier = "test_finish_token_distr_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayProject(
                getWcaContract(), "description",
                getCatTokenAddress(), stakeRate, totalAmount,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, false,
                identifier, this.creatorAccount
        );

        // purchase
        // NOTE: one purchase per WCA per block. Since only one write operation will be accepted 
        //       by committee, rest of them will be discarded and become invalid.
        transferToken(
                getCatToken(), this.buyerAccount1,
                getWcaContractAddress(),
                buyer1Purchase, identifier, true
        );
        transferToken(
                getCatToken(), this.buyerAccount2,
                getWcaContractAddress(),
                buyer2Purchase, identifier, true
        );

        var creatorOldBalance = getCatToken().getBalanceOf(this.creatorAccount).longValue();
        var buyer1OldBalance = getCatToken().getBalanceOf(this.buyerAccount1).longValue();
        var buyer2OldBalance = getCatToken().getBalanceOf(this.buyerAccount2).longValue();
        // buyer1: 400.00, buyer2: 500.00, remain: 100.0
        // finish ms [0] and [2], thus creator lose 1/3 token
        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "something", this.creatorAccount
        );
        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 2, "something", this.creatorAccount
        );
        var creatorNewBalance = getCatToken().getBalanceOf(this.creatorAccount).longValue();
        var buyer1NewBalance = getCatToken().getBalanceOf(this.buyerAccount1).longValue();
        var buyer2NewBalance = getCatToken().getBalanceOf(this.buyerAccount2).longValue();

        // buy this time:
        var buyer1Total = buyer1Purchase + buyer1Purchase * stakeRate / 100;
        var buyer1Return = buyer1Total / 3;
        assertEquals(buyer1OldBalance + buyer1Return, buyer1NewBalance);
        var buyer2Total = buyer2Purchase + buyer2Purchase * stakeRate / 100;
        var buyer2Return = buyer2Total / 3;
        assertEquals(buyer2OldBalance + buyer2Return, buyer2NewBalance);
        // creator get purchased and remain staked
        var creatorGet = (buyer1Total - buyer1Return) + (buyer2Total - buyer2Return)
                + (totalAmount - buyer1Purchase - buyer2Purchase) * stakeRate / 100;
        assertEquals(creatorOldBalance + creatorGet, creatorNewBalance);
    }
}
