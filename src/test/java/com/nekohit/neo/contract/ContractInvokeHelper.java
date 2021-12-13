package com.nekohit.neo.contract;

import io.neow3j.contract.SmartContract;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * This class offers some handy functions for invoking WCAContract.
 */
public class ContractInvokeHelper {
    /**
     * Query WCA details, return NeoVM json
     */
    public static String queryProject(SmartContract contract, String trueId) throws Throwable {
        var result = ContractTestFramework.testInvoke(
                contract, "queryProject",
                new ContractParameter[]{
                        ContractParameter.string(trueId)
                },
                new Signer[0]
        );
        return result.getStack().get(0).getString();
    }

    public static BigInteger queryPurchase(
            SmartContract contract, String identifier, Account buyer
    ) throws Throwable {
        var result = ContractTestFramework.testInvoke(
                contract, "queryPurchase",
                new ContractParameter[]{
                        ContractParameter.string(identifier),
                        ContractParameter.hash160(buyer)
                },
                new Signer[0]
        );
        return result.getStack().get(0).getInteger();
    }

    public static String advanceQuery(
            SmartContract contract, Hash160 token, Hash160 creator, Hash160 buyer, int page, int size
    ) throws Throwable {
        var result = ContractTestFramework.testInvoke(
                contract, "advanceQuery",
                new ContractParameter[]{
                        ContractParameter.hash160(token),
                        ContractParameter.hash160(creator),
                        ContractParameter.hash160(buyer),
                        ContractParameter.integer(page),
                        ContractParameter.integer(size)
                },
                new Signer[0]
        );
        return result.getStack().get(0).getString();
    }

    /**
     * Create WCA, return the identifier
     */
    public static String declareProject(
            SmartContract contract, String wcaDescription,
            Hash160 token, int stakeRate100, long totalAmount,
            String[] milestoneTitles, String[] milestoneDescriptions, Long[] endTimestamps,
            int thresholdIndex, long coolDownInterval, boolean bePublic,
            String identifier, Account account
    ) throws Throwable {
        var appLog = ContractTestFramework.invokeFunction(
                contract, "declareProject",
                new ContractParameter[]{
                        ContractParameter.hash160(account),
                        ContractParameter.string(wcaDescription),
                        ContractParameter.hash160(token),
                        ContractParameter.integer(stakeRate100),
                        ContractParameter.integer(BigInteger.valueOf(totalAmount)),
                        ContractParameter.array(Arrays.asList(milestoneTitles)),
                        ContractParameter.array(Arrays.asList(milestoneDescriptions)),
                        ContractParameter.array(Arrays.asList(endTimestamps)),
                        ContractParameter.integer(thresholdIndex),
                        ContractParameter.integer(BigInteger.valueOf(coolDownInterval)),
                        ContractParameter.bool(bePublic),
                        ContractParameter.string(identifier)
                },
                new Signer[]{
                        AccountSigner.calledByEntry(account)
                }
        );
        return appLog.getExecutions().get(0).getStack().get(0).getString();
    }

    public static String createAndPayProject(
            SmartContract contract, String wcaDescription,
            Hash160 token, int stakePer100Token, long totalAmount,
            String[] milestoneTitles, String[] milestoneDescriptions, Long[] endTimestamps,
            int thresholdIndex, long coolDownInterval, boolean bePublic,
            String identifier, Account account
    ) throws Throwable {
        var result = declareProject(
                contract, wcaDescription, token, stakePer100Token, totalAmount,
                milestoneTitles, milestoneDescriptions, endTimestamps,
                thresholdIndex, coolDownInterval, bePublic, identifier, account
        );

        // pay stake
        ContractTestFramework.transferToken(
                ContractTestFramework.tokenFromAddress(token), account,
                ContractTestFramework.getWcaContractAddress(),
                stakePer100Token * totalAmount / 100,
                identifier, true
        );

        return result;
    }

    public static void finishMilestone(
            SmartContract contract, String identifier,
            int index, String proofOfWork, Account account
    ) throws Throwable {
        ContractTestFramework.invokeFunction(
                contract, "finishMilestone",
                new ContractParameter[]{
                        ContractParameter.string(identifier),
                        ContractParameter.integer(index),
                        ContractParameter.string(proofOfWork)
                },
                new Signer[]{
                        AccountSigner.calledByEntry(account)
                }
        );
    }

    public static void refund(
            SmartContract contract, String identifier, Account buyerAccount
    ) throws Throwable {
        ContractTestFramework.invokeFunction(
                contract, "refund",
                new ContractParameter[]{
                        ContractParameter.string(identifier),
                        ContractParameter.hash160(buyerAccount)
                },
                new Signer[]{
                        AccountSigner.calledByEntry(buyerAccount)
                }
        );
    }

    public static void finishProject(
            SmartContract contract, String identifier, Account account
    ) throws Throwable {
        ContractTestFramework.invokeFunction(
                contract, "finishProject",
                new ContractParameter[]{
                        ContractParameter.string(identifier)
                },
                new Signer[]{
                        AccountSigner.calledByEntry(account)
                }
        );
    }

    public static void cancelProject(
            SmartContract contract, String identifier, Account account
    ) throws Throwable {
        ContractTestFramework.invokeFunction(
                contract, "cancelProject",
                new ContractParameter[]{
                        ContractParameter.string(identifier)
                },
                new Signer[]{
                        AccountSigner.calledByEntry(account)
                }
        );
    }
}
