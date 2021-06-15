package info.skyblond.nekohit.example.contract;

import java.math.BigInteger;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.skyblond.nekohit.example.Constants;
import info.skyblond.nekohit.example.Utils;
import info.skyblond.nekohit.neo.helper.Pair;
import io.neow3j.contract.SmartContract;
import io.neow3j.transaction.Transaction;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Wallet;

public class ContractHelper {
    private static final Logger logger = LoggerFactory.getLogger(ContractHelper.class);

    private static Pair<Transaction, NeoSendRawTransaction> buildTxAndSend(
            SmartContract contract, String function,
            ContractParameter[] parameters, Signer[] signers,
            Wallet wallet
    ) throws Throwable {
        var tx = contract
                .invokeFunction(function, parameters)
                .signers(signers).wallet(wallet).sign();
        var response = tx.send();
        return new Pair<>(tx, response);
    }
    
    public static NeoApplicationLog invokeFunction(
        SmartContract contract, String function, 
        ContractParameter[] parameters, Signer[] signers, Wallet wallet
    ) throws Throwable {
        var txAndResp = buildTxAndSend(contract, function, parameters, signers, wallet);
        if (txAndResp.second.hasError()) {
            throw new Exception(String.format("Error when invoking %s: %s", function, txAndResp.second.getError().getMessage()));
        }
        logger.info("{} tx: {}", function, txAndResp.first.getTxId());
        Await.waitUntilTransactionIsExecuted(txAndResp.first.getTxId(), Constants.NEOW3J);
        logger.info("{} gas fee: {}", function, Utils.getGasWithDecimals(txAndResp.first.getSystemFee() + txAndResp.first.getNetworkFee()));
        return txAndResp.first.getApplicationLog();
    }

    
    /**
     * Create WCA with Contract Owner account
     */
    public static String createWCA(
        SmartContract contract, int stakePer100Token, long totalAmount,
        String[] descriptions, Long[] endTimestamps, int thresholdIndex, String identifier
    ) throws Throwable {

        // expired in 30 hours
        for (int i = 0; i < endTimestamps.length; i++) {
            endTimestamps[i] = System.currentTimeMillis() + 1800 * 1000 + i;
        }

        var appLog = ContractHelper.invokeFunction(
            contract, "createWCA", 
            new ContractParameter[]{
                ContractParameter.hash160(Constants.CONTRACT_OWNER_ACCOUNT),
                ContractParameter.integer(stakePer100Token), 
                ContractParameter.integer(BigInteger.valueOf(totalAmount)),
                ContractParameter.array(Arrays.asList(descriptions)),
                ContractParameter.array(Arrays.asList(endTimestamps)),
                ContractParameter.integer(thresholdIndex),
                ContractParameter.string(identifier)
            }, 
            new Signer[] {
                Signer.calledByEntry(Constants.CONTRACT_OWNER_ACCOUNT)
            }, 
            Constants.CONTRACT_OWNER_WALLET
        );
        return appLog.getExecutions().get(0).getStack().get(0).getString();
    }

    public static void finishMilestone(
        SmartContract contract, String identifier, 
        int index, String proofOfWork
    ) throws Throwable {
        ContractHelper.invokeFunction(
            contract, "finishMilestone", 
            new ContractParameter[]{
                ContractParameter.string(identifier),
                ContractParameter.integer(index), 
                ContractParameter.string(proofOfWork)
            }, 
            new Signer[] {
                Signer.calledByEntry(Constants.CONTRACT_OWNER_ACCOUNT)
            }, 
            Constants.CONTRACT_OWNER_WALLET
        );
    }

    public static void finishWCA(
        SmartContract contract, String identifier
    ) throws Throwable {
        ContractHelper.invokeFunction(
            contract, "finishWCA", 
            new ContractParameter[]{
                ContractParameter.string(identifier)
            }, 
            new Signer[] {
                Signer.calledByEntry(Constants.CONTRACT_OWNER_ACCOUNT)
            }, 
            Constants.CONTRACT_OWNER_WALLET
        );
    }

    public static void refund(
        SmartContract contract, String identifier, Wallet buyerWallet
    ) throws Throwable {
        ContractHelper.invokeFunction(
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

    public static String queryWCAJson(SmartContract contract, String trueId) throws Throwable {
        var appLog = ContractHelper.invokeFunction(
            contract, "queryWCA", 
            new ContractParameter[]{
                ContractParameter.string(trueId)
            }, 
            new Signer[] {
                Signer.calledByEntry(Constants.CONTRACT_OWNER_ACCOUNT)
            }, 
            Constants.CONTRACT_OWNER_WALLET
        );
        return appLog.getExecutions().get(0).getStack().get(0).getString();
    }

}
