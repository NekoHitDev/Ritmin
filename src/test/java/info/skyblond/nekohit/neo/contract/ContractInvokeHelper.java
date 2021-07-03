package info.skyblond.nekohit.neo.contract;

import io.neow3j.contract.SmartContract;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * This class offers some handy functions for invoking WCAContract.
 */
public class ContractInvokeHelper {


    // ---------------- todo ----------------

    /**
     * Query WCA details, return NeoVM json
     */
    public static String queryWCA(SmartContract contract, String trueId) throws Throwable {
        var result = ContractTestFramework.testInvoke(
                contract, "queryWCA",
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
            SmartContract contract, Hash160 creator, Hash160 buyer, int page, int size
    ) throws Throwable {
        var result = ContractTestFramework.testInvoke(
                contract, "advanceQuery",
                new ContractParameter[]{
                        ContractParameter.hash160(creator),
                        ContractParameter.hash160(buyer),
                        ContractParameter.integer(page),
                        ContractParameter.integer(size)
                },
                new Signer[0]
        );
        return result.getStack().get(0).getString();
    }

    // ---------------- todo ----------------

    /**
     * Create WCA, return the identifier
     */
    public static String createWCA(

            // Hash160 owner, String wcaDescription,
            //        int stakePer100Token, int maxTokenSoldCount,
            //        String[] milestoneTitles, String[] milestoneDescriptions, int[] endTimestamps,
            //        int thresholdIndex, int coolDownInterval,
            //        boolean bePublic, String identifier
            SmartContract contract, String wcaDescription,
            int stakePer100Token, long totalAmount,
            String[] milestoneTitles, String[] milestoneDescriptions, Long[] endTimestamps,
            int thresholdIndex, long coolDownInterval, boolean bePublic,
            String identifier, Wallet wallet
    ) throws Throwable {
        var appLog = ContractTestFramework.invokeFunction(
                contract, "createWCA",
                new ContractParameter[]{
                        ContractParameter.hash160(wallet.getDefaultAccount()),
                        ContractParameter.string(wcaDescription),
                        ContractParameter.integer(stakePer100Token),
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
                        Signer.calledByEntry(wallet.getDefaultAccount())
                },
                wallet
        );
        return appLog.getExecutions().get(0).getStack().get(0).getString();
    }

    public static String createAndPayWCA(
            SmartContract contract, String wcaDescription,
            int stakePer100Token, long totalAmount,
            String[] milestoneTitles, String[] milestoneDescriptions, Long[] endTimestamps,
            int thresholdIndex, long coolDownInterval, boolean bePublic,
            String identifier, Wallet wallet
    ) throws Throwable {
        var result = createWCA(
                contract, wcaDescription, stakePer100Token, totalAmount,
                milestoneTitles, milestoneDescriptions, endTimestamps,
                thresholdIndex, coolDownInterval, bePublic, identifier, wallet
        );

        // pay stake
        ContractTestFramework.transferToken(
                ContractTestFramework.getCatToken(), wallet,
                ContractTestFramework.getWcaContractAddress(),
                stakePer100Token * totalAmount / 100,
                identifier, true
        );

        return result;
    }

    public static void finishMilestone(
            SmartContract contract, String identifier,
            int index, String proofOfWork, Wallet wallet
    ) throws Throwable {
        ContractTestFramework.invokeFunction(
                contract, "finishMilestone",
                new ContractParameter[]{
                        ContractParameter.string(identifier),
                        ContractParameter.integer(index),
                        ContractParameter.string(proofOfWork)
                },
                new Signer[]{
                        Signer.calledByEntry(wallet.getDefaultAccount())
                },
                wallet
        );
    }

    public static void refund(
            SmartContract contract, String identifier, Wallet buyerWallet
    ) throws Throwable {
        ContractTestFramework.invokeFunction(
                contract, "refund",
                new ContractParameter[]{
                        ContractParameter.string(identifier),
                        ContractParameter.hash160(buyerWallet.getDefaultAccount())
                },
                new Signer[]{
                        Signer.calledByEntry(buyerWallet.getDefaultAccount())
                },
                buyerWallet
        );
    }

    public static void finishWCA(
            SmartContract contract, String identifier, Wallet wallet
    ) throws Throwable {
        ContractTestFramework.invokeFunction(
                contract, "finishWCA",
                new ContractParameter[]{
                        ContractParameter.string(identifier)
                },
                new Signer[]{
                        Signer.calledByEntry(wallet.getDefaultAccount())
                },
                wallet
        );
    }
}
