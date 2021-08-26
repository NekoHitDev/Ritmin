package com.nekohit.neo.contract;

import com.nekohit.neo.domain.ExceptionMessages;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.ContractParameter;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class test the creation of WCA.
 * Including invalid parameter, invalid milestones, duplicate identifier, etc.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class WCACreateTest extends ContractTestFramework {
    private final Wallet testWallet = getTestWallet();

    @Test
    void testNegativeStakeRate() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        -100, 10,
                        new String[]{"milestone"},
                        new String[]{"milestone"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000},
                        0, 100, false,
                        "test_negative_stake_rate_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STAKE_RATE),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testZeroStakeRate() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        0, 10,
                        new String[]{"milestone"},
                        new String[]{"milestone"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000},
                        0, 100, false,
                        "test_zero_stake_rate_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_STAKE_RATE),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNegativeTokenCount() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        100, -100,
                        new String[]{"milestone"},
                        new String[]{"milestone"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000},
                        0, 100, false,
                        "test_negative_token_count_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_MAX_SELL_AMOUNT),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testZeroTokenCount() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        100, 0,
                        new String[]{"milestone"},
                        new String[]{"milestone"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000},
                        0, 100, false,
                        "test_zero_token_count_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_MAX_SELL_AMOUNT),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidSigner() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> {
                    ContractTestFramework.invokeFunction(
                            getWcaContract(), "createWCA",
                            new ContractParameter[]{
                                    ContractParameter.hash160(Account.create()),
                                    ContractParameter.string("Something"),
                                    ContractParameter.integer(100),
                                    ContractParameter.integer(1000),
                                    ContractParameter.array(Arrays.asList("milestone")),
                                    ContractParameter.array(Arrays.asList("milestone")),
                                    ContractParameter.array(Arrays.asList(System.currentTimeMillis() + 60 * 1000)),
                                    ContractParameter.integer(0),
                                    ContractParameter.integer(100),
                                    ContractParameter.bool(false),
                                    ContractParameter.string("test_invalid_signer_" + System.currentTimeMillis())
                            },
                            new Signer[]{
                                    AccountSigner.calledByEntry(this.testWallet.getDefaultAccount())
                            },
                            this.testWallet
                    );
                }
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_SIGNATURE),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testDuplicateIdentifier() throws Throwable {
        var identifier = "test_duplicate_id_" + System.currentTimeMillis();
        ContractInvokeHelper.createWCA(
                getWcaContract(), "description",
                100, 1000,
                new String[]{"milestone"},
                new String[]{"milestone"},
                new Long[]{System.currentTimeMillis() + 60 * 1000},
                0, 100, false,
                identifier, this.testWallet
        );
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        100, 0,
                        new String[]{"milestone"},
                        new String[]{"milestone"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000},
                        0, 100, false,
                        identifier, this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.DUPLICATED_ID),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testDifferentMilestoneCount() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        100, 1000,
                        new String[]{"milestone1", "milestone2"},
                        new String[]{"milestone1", "milestone2"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000},
                        0, 100, false,
                        "test_different_milestone_count_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_MILESTONES_COUNT),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testDecreaseEndTimestamp() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        100, 1000,
                        new String[]{"milestone1", "milestone2"},
                        new String[]{"milestone1", "milestone2"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000, System.currentTimeMillis() + 59 * 1000},
                        0, 100, false,
                        "test_decrease_end_timestamp_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_TIMESTAMP),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testExpiredEndTimestamp() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        100, 1000,
                        new String[]{"milestone1"},
                        new String[]{"milestone1"},
                        // definitely a expired timestamp
                        new Long[]{12345L},
                        0, 100, false,
                        "test_expired_end_timestamp_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.EXPIRED_TIMESTAMP),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    // TODO cannot test zero milestone, 
    //      since neow3j require at least 1 element in a given array

    @Test
    void testInvalidThresholdMilestone() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        100, 1000,
                        new String[]{"milestone1"},
                        new String[]{"milestone1"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000},
                        -1, 100, false,
                        "test_invalid_threshold_milestone_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_THRESHOLD_INDEX),
                "Unknown exception: " + throwable.getMessage()
        );

        throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        100, 1000,
                        new String[]{"milestone1"},
                        new String[]{"milestone1"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000},
                        1, 100, false,
                        "test_invalid_threshold_milestone_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_THRESHOLD_INDEX),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testInvalidCoolDownInterval() throws Throwable {
        var throwable = assertThrows(
                TransactionConfigurationException.class,
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        100, 1000,
                        new String[]{"milestone1"},
                        new String[]{"milestone1"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000},
                        0, -100, false,
                        "test_invalid_cool_down_interval_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
        assertTrue(
                throwable.getMessage().contains(ExceptionMessages.INVALID_COOL_DOWN_INTERVAL),
                "Unknown exception: " + throwable.getMessage()
        );
    }

    @Test
    void testNormalOperation() throws Throwable {
        assertDoesNotThrow(
                () -> ContractInvokeHelper.createWCA(
                        getWcaContract(), "description",
                        100, 1000,
                        new String[]{"milestone1"},
                        new String[]{"milestone1"},
                        new Long[]{System.currentTimeMillis() + 60 * 1000},
                        0, 100, false,
                        "test_create_normal_" + System.currentTimeMillis(),
                        this.testWallet
                )
        );
    }
}
