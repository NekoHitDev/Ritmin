package info.skyblond.nekohit.test.wca;

import java.math.BigInteger;
import java.util.Arrays;

import info.skyblond.nekohit.test.ContractTestFramework;
import io.neow3j.contract.SmartContract;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.wallet.Wallet;

/**
 * This class offers some handy functions for invoking WCAContract.
 * 
 * TODO WCA Test: purchase, finish ms, finish wca, refund, query
 */
public class ContractInvokeHelper {

    /**
     * Query WCA details, return NeoVM json
     */
    public static String queryWCAJson(SmartContract contract, String trueId) throws Throwable {
        var result = ContractTestFramework.testInvoke(
            contract, "queryWCA", 
            new ContractParameter[]{
                ContractParameter.string(trueId)
            }, 
            new Signer[0]
        );
        return result.getStack().get(0).getString();
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
}
