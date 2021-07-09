package info.skyblond.nekohit.neo.contract;

import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.wallet.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test the finish wca method for WCA.
 * Including invalid id, unpaid, not ready to finish(last ms not finished nor expired),
 * owner override, double finished, normal op(check token distribution)
 */
@TestInstance(Lifecycle.PER_CLASS)
public class WCAFinishWCATest extends ContractTestFramework {
    private final Wallet creatorWallet = getTestWallet();
    private final Wallet buyerWallet1 = getTestWallet();
    private final Wallet buyerWallet2 = getTestWallet();

    @Test
    void testInvalidId() {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishWCA(
                        getWcaContract(), "some_invalid_id", this.creatorWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains("Identifier not found."),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishUnpaid() throws Throwable {
        var identifier = "test_finish_unpaid_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishWCA(
                        getWcaContract(), identifier, this.creatorWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains("You can not finish an unpaid WCA."),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNotFinished() throws Throwable {
        var identifier = "test_finish_not_ready_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishWCA(
                        getWcaContract(), identifier, this.buyerWallet1
                )
        );
        assertTrue(
                throwable.getMessage().contains("You can only apply this to a ready-to-finish WCA."),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testOwnerOverride() throws Throwable {
        var identifier = "test_finish_owner_override_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );

        assertDoesNotThrow(
                () -> ContractInvokeHelper.finishWCA(
                        getWcaContract(), identifier, this.creatorWallet
                )
        );
    }

    @Test
    void testDoubleFinish() throws Throwable {
        var identifier = "test_double_finish_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );

        ContractInvokeHelper.finishWCA(
                getWcaContract(), identifier, this.creatorWallet
        );

        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishWCA(
                        getWcaContract(), identifier, this.buyerWallet1
                )
        );
        assertTrue(
                throwable.getMessage().contains("You can not finish a WCA twice."),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishFinished() throws Throwable {
        var identifier = "test_finish_finished_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );

        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "something", this.creatorWallet
        );
        // By finish the last ms, WCA is finished.
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.finishWCA(
                        getWcaContract(), identifier, this.buyerWallet1
                )
        );
        assertTrue(
                throwable.getMessage().contains("You can not finish a WCA twice."),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testFinishExpired() throws Throwable {
        var identifier = "test_finish_expired_wca_" + System.currentTimeMillis();
        // create WCA
        ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                1_00, 1_00,
                new String[]{"milestone1"},
                new String[]{"milestone1"},
                new Long[]{System.currentTimeMillis() + 2 * 1000},
                0, 100, false,
                identifier, this.creatorWallet
        );

        // let the last one expire
        Thread.sleep(2 * 1000);

        assertDoesNotThrow(
                () -> ContractInvokeHelper.finishWCA(
                        getWcaContract(), identifier, this.buyerWallet1
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
        ContractInvokeHelper.createAndPayWCA(
                getWcaContract(), "description",
                stakeRate, totalAmount,
                new String[]{"milestone1", "milestone2", "milestone3"},
                new String[]{"milestone1", "milestone2", "milestone3"},
                new Long[]{
                        System.currentTimeMillis() + 60 * 1000,
                        System.currentTimeMillis() + 61 * 1000,
                        System.currentTimeMillis() + 62 * 1000
                },
                0, 1, false,
                identifier, this.creatorWallet
        );

        // purchase
        // NOTE: one purchase per WCA per block. Since only one write operation will be accepted 
        //       by committee, rest of them will be discarded and become invalid.
        transferToken(
                getCatToken(), this.buyerWallet1,
                getWcaContractAddress(),
                buyer1Purchase, identifier, true
        );
        transferToken(
                getCatToken(), this.buyerWallet2,
                getWcaContractAddress(),
                buyer2Purchase, identifier, true
        );

        var creatorOldBalance = getCatToken().getBalanceOf(this.creatorWallet.getDefaultAccount()).longValue();
        var buyer1OldBalance = getCatToken().getBalanceOf(this.buyerWallet1.getDefaultAccount()).longValue();
        var buyer2OldBalance = getCatToken().getBalanceOf(this.buyerWallet2.getDefaultAccount()).longValue();
        // buyer1: 400.00, buyer2: 500.00, remain: 100.0
        // finish ms [0] and [2], thus creator lose 1/3 token
        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 0, "something", this.creatorWallet
        );
        ContractInvokeHelper.finishMilestone(
                getWcaContract(), identifier, 2, "something", this.creatorWallet
        );
        var creatorNewBalance = getCatToken().getBalanceOf(this.creatorWallet.getDefaultAccount()).longValue();
        var buyer1NewBalance = getCatToken().getBalanceOf(this.buyerWallet1.getDefaultAccount()).longValue();
        var buyer2NewBalance = getCatToken().getBalanceOf(this.buyerWallet2.getDefaultAccount()).longValue();

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
