package info.skyblond.nekohit.test.wca;

import java.math.BigInteger;
import java.util.Arrays;

import info.skyblond.nekohit.test.ContractTestFramework;
import io.neow3j.contract.SmartContract;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;

/**
 * This class offers some handy functions for invoking WCAContract.
 */
public class ContractInvokeHelper {

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

    /**
     * Create WCA, return the identifier
     */
    public static String createWCA(
        SmartContract contract, int stakePer100Token, long totalAmount,
        String[] descriptions, Long[] endTimestamps, int thresholdIndex, 
        long coolDownInterval, String identifier, Wallet wallet
    ) throws Throwable {
        var appLog = ContractTestFramework.invokeFunction(
            contract, "createWCA", 
            new ContractParameter[]{
                ContractParameter.hash160(wallet.getDefaultAccount()),
                ContractParameter.integer(stakePer100Token), 
                ContractParameter.integer(BigInteger.valueOf(totalAmount)),
                ContractParameter.array(Arrays.asList(descriptions)),
                ContractParameter.array(Arrays.asList(endTimestamps)),
                ContractParameter.integer(thresholdIndex),
                ContractParameter.integer(BigInteger.valueOf(coolDownInterval)),
                ContractParameter.string(identifier)
            }, 
            new Signer[] {
                Signer.calledByEntry(wallet.getDefaultAccount())
            }, 
            wallet
        );
        return appLog.getExecutions().get(0).getStack().get(0).getString();
    }

    public static String createAndPayWCA(
        SmartContract contract, int stakePer100Token, long totalAmount,
        String[] descriptions, Long[] endTimestamps, int thresholdIndex, 
        long coolDownInterval, String identifier, Wallet wallet
    ) throws Throwable {
        var result = createWCA(
            contract, stakePer100Token, totalAmount, descriptions, endTimestamps, 
            thresholdIndex, coolDownInterval, identifier, wallet
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
            new Signer[] {
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
            new Signer[] {
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
            new Signer[] {
                Signer.calledByEntry(wallet.getDefaultAccount())
            }, 
            wallet
        );
    }
}
