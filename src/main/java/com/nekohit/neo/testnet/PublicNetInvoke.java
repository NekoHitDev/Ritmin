package com.nekohit.neo.testnet;

import com.nekohit.neo.helper.Utils;
import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoInvokeFunction;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;

@SuppressWarnings({"unused", "SameParameterValue"})
public final class PublicNetInvoke {
    private static final Logger logger = LoggerFactory.getLogger(PublicNetInvoke.class);

    private static final Neow3j NEOW3J = Neow3j.build(new HttpService("http://seed2t.neo.org:20332"));
    private static final GasToken GAS_TOKEN = new GasToken(NEOW3J);
    private static final FungibleToken CAT_TOKEN = new FungibleToken(new Hash160("0xf461dff74f454e5016421341f115a2e789eadbd7"), NEOW3J);
    private static final SmartContract WCA_CONTRACT = new SmartContract(new Hash160("0xbb1b061b381ccbee925909709be2ef37ece3e6c8"), NEOW3J);

    private static Wallet wallet;

    public static void main(String[] args) throws Throwable {
        wallet = Utils.readWalletWIF();

        String id = createAndPayWCA(
                "中文测试！！！",
                1, 100_00,
                new String[]{"MS1", "MS2", "MS3"},
                new String[]{"milestone #1", "milestone #2", "milestone #3"},
                new Long[]{
                        System.currentTimeMillis() + 1 * 30L * 24 * 60 * 60 * 1000,
                        System.currentTimeMillis() + 2 * 30L * 24 * 60 * 60 * 1000,
                        System.currentTimeMillis() + 3 * 30L * 24 * 60 * 60 * 1000
                },
                0, 100,
                true, "中文标签测试_" + System.currentTimeMillis()
        );
        System.out.println(id);
    }

    private static double getGasFeeFromTx(Transaction tx) {
        long fraction = tx.getSystemFee() + tx.getNetworkFee();
        return fraction / Math.pow(10, 8);
    }

    private static NeoApplicationLog invokeWCA(
            String function, ContractParameter... parameters
    ) throws Throwable {
        Transaction tx = WCA_CONTRACT
                .invokeFunction(function, parameters)
                .signers(AccountSigner.calledByEntry(wallet.getDefaultAccount()))
//                .wallet(wallet)
                .sign();
        NeoSendRawTransaction response = tx.send();
        if (response.hasError()) {
            throw new Exception(String.format("Error when invoking %s: %s", function, response.getError().getMessage()));
        }
        logger.info("{} tx: {}", function, tx.getTxId());
        Await.waitUntilTransactionIsExecuted(tx.getTxId(), NEOW3J);
        logger.info("{} gas fee: {}", function, getGasFeeFromTx(tx));
        NeoApplicationLog appLog = tx.getApplicationLog();
        if (appLog.getExecutions().get(0).getState() != NeoVMStateType.HALT) {
            throw new Exception(appLog.getExecutions().get(0).getException());
        }
        return appLog;
    }

    private static InvocationResult testInvoke(
            String function, ContractParameter... parameters
    ) throws Exception {
        NeoInvokeFunction tx = WCA_CONTRACT.callInvokeFunction(function, Arrays.asList(parameters), AccountSigner.calledByEntry(wallet.getDefaultAccount()));
        if (tx.hasError()) {
            throw new Exception(String.format("Error when test invoking %s: %s", function, tx.getError().getMessage()));
        }
        if (tx.getInvocationResult().hasStateFault()) {
            throw new Exception(tx.getInvocationResult().getException());
        }
        return tx.getInvocationResult();
    }

    private static String queryWCA(String trueId) throws Throwable {
        InvocationResult result = testInvoke("queryWCA",
                ContractParameter.string(trueId));
        return result.getStack().get(0).getString();
    }

    private static BigInteger queryPurchase(
            String identifier, Account buyer
    ) throws Throwable {
        InvocationResult result = testInvoke("queryPurchase",
                ContractParameter.string(identifier),
                ContractParameter.hash160(buyer));
        return result.getStack().get(0).getInteger();
    }

    private static String advanceQuery(
            Hash160 creator, Hash160 buyer, int page, int size
    ) throws Throwable {
        InvocationResult result = testInvoke("advanceQuery",
                ContractParameter.hash160(creator),
                ContractParameter.hash160(buyer),
                ContractParameter.integer(page),
                ContractParameter.integer(size));
        return result.getStack().get(0).getString();
    }


    private static String createWCA(
            String wcaDescription, int stakePer100Token, long maxTokenSoldCount,
            String[] milestoneTitles, String[] milestoneDescriptions, Long[] endTimestamps,
            int thresholdIndex, long coolDownInterval,
            boolean bePublic, String identifier
    ) throws Throwable {
        NeoApplicationLog appLog = invokeWCA("createWCA",
                ContractParameter.hash160(wallet.getDefaultAccount()),
                ContractParameter.string(wcaDescription),
                ContractParameter.integer(stakePer100Token),
                ContractParameter.integer(BigInteger.valueOf(maxTokenSoldCount)),
                ContractParameter.array(Arrays.asList(milestoneTitles)),
                ContractParameter.array(Arrays.asList(milestoneDescriptions)),
                ContractParameter.array(Arrays.asList(endTimestamps)),
                ContractParameter.integer(thresholdIndex),
                ContractParameter.integer(BigInteger.valueOf(coolDownInterval)),
                ContractParameter.bool(bePublic),
                ContractParameter.string(identifier));
        return appLog.getExecutions().get(0).getStack().get(0).getString();
    }

    private static void transferCatToken(
            Hash160 to, long amount, String identifier, boolean wait
    ) throws Throwable {
        NeoSendRawTransaction tx = CAT_TOKEN.transfer(
                wallet.getDefaultAccount(), to, BigInteger.valueOf(amount), ContractParameter.string(identifier)
        ).sign().send();

        if (tx.hasError()) {
            throw new Exception(tx.getError().getMessage());
        }

        if (wait) {
            Await.waitUntilTransactionIsExecuted(tx.getSendRawTransaction().getHash(), NEOW3J);
        }

        logger.info("Transfer {} {} from {} to {}, tx: {}",
                amount, PublicNetInvoke.CAT_TOKEN.getSymbol(),
                wallet.getDefaultAccount().getAddress(),
                to.toAddress(), tx.getSendRawTransaction().getHash());
    }

    private static String createAndPayWCA(
            String wcaDescription, int stakePer100Token, long maxTokenSoldCount,
            String[] milestoneTitles, String[] milestoneDescriptions, Long[] endTimestamps,
            int thresholdIndex, long coolDownInterval,
            boolean bePublic, String identifier
    ) throws Throwable {
        String result = createWCA(wcaDescription, stakePer100Token, maxTokenSoldCount,
                milestoneTitles, milestoneDescriptions, endTimestamps,
                thresholdIndex, coolDownInterval, bePublic, identifier);

        // pay stake
        transferCatToken(WCA_CONTRACT.getScriptHash(),
                stakePer100Token * maxTokenSoldCount / 100,
                identifier, true);

        return result;
    }

    private static void finishMilestone(
            String identifier,
            int index, String proofOfWork
    ) throws Throwable {
        invokeWCA("finishMilestone",
                ContractParameter.string(identifier),
                ContractParameter.integer(index),
                ContractParameter.string(proofOfWork));
    }

    private static void refund(
            String identifier
    ) throws Throwable {
        invokeWCA("refund",
                ContractParameter.string(identifier),
                ContractParameter.hash160(wallet.getDefaultAccount()));
    }

    private static void finishWCA(String identifier) throws Throwable {
        invokeWCA("finishWCA", ContractParameter.string(identifier));
    }
}
