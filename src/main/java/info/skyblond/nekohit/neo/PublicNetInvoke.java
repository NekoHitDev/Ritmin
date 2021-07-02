package info.skyblond.nekohit.neo;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.NeoVMStateType;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;

@SuppressWarnings({"unused", "SameParameterValue"})
public final class PublicNetInvoke {
    private static final Logger logger = LoggerFactory.getLogger(PublicNetInvoke.class);

    private static final Neow3j NEOW3J = Neow3j.build(new HttpService("http://seed1t.neo.org:20332"));
    private static final GasToken GAS_TOKEN = new GasToken(NEOW3J);
    private static final FungibleToken CAT_TOKEN = new FungibleToken(new Hash160("0xf461dff74f454e5016421341f115a2e789eadbd7"), NEOW3J);
    private static final SmartContract WCA_CONTRACT = new SmartContract(new Hash160("0x11ed46dd463f850b628b27e632532157fb6200bd"), NEOW3J);

    private static Wallet wallet;

    public static void main(String[] args) throws Throwable {
        String result = advanceQuery(
                Hash160.ZERO, Hash160.ZERO,
                true, false, false, false,
                1, 20
        );
        System.out.println(result);
    }

    private static double getGasFeeFromTx(Transaction tx) {
        var fraction = tx.getSystemFee() + tx.getNetworkFee();
        return fraction / Math.pow(10, 8);
    }

    private static NeoApplicationLog invokeWCA(
            String function, ContractParameter... parameters
    ) throws Throwable {
        var tx = WCA_CONTRACT
                .invokeFunction(function, parameters)
                .signers(Signer.calledByEntry(wallet.getDefaultAccount())).wallet(wallet).sign();
        var response = tx.send();
        if (response.hasError()) {
            throw new Exception(String.format("Error when invoking %s: %s", function, response.getError().getMessage()));
        }
        logger.info("{} tx: {}", function, tx.getTxId());
        Await.waitUntilTransactionIsExecuted(tx.getTxId(), NEOW3J);
        logger.info("{} gas fee: {}", function, getGasFeeFromTx(tx));
        var appLog = tx.getApplicationLog();
        if (appLog.getExecutions().get(0).getState() != NeoVMStateType.HALT)
            throw new Exception(appLog.getExecutions().get(0).getException());
        return appLog;
    }

    private static InvocationResult testInvoke(
            String function, ContractParameter... parameters
    ) throws Exception {
        var tx = WCA_CONTRACT.callInvokeFunction(function, Arrays.asList(parameters), Signer.calledByEntry(wallet.getDefaultAccount()));
        if (tx.hasError())
            throw new Exception(String.format("Error when test invoking %s: %s", function, tx.getError().getMessage()));
        return tx.getInvocationResult();
    }

    private static String queryWCA(String trueId) throws Throwable {
        var result = testInvoke("queryWCA",
                ContractParameter.string(trueId));
        return result.getStack().get(0).getString();
    }

    private static BigInteger queryPurchase(
            String identifier, Account buyer
    ) throws Throwable {
        var result = testInvoke("queryPurchase",
                ContractParameter.string(identifier),
                ContractParameter.hash160(buyer));
        return result.getStack().get(0).getInteger();
    }

    private static String advanceQuery(
            Hash160 creator, Hash160 buyer,
            boolean unpaid, boolean canPurchase, boolean onGoing, boolean finished,
            int page, int size
    ) throws Throwable {
        var result = testInvoke("advanceQuery",
                ContractParameter.hash160(creator),
                ContractParameter.hash160(buyer),
                ContractParameter.bool(unpaid),
                ContractParameter.bool(canPurchase),
                ContractParameter.bool(onGoing),
                ContractParameter.bool(finished),
                ContractParameter.integer(page),
                ContractParameter.integer(size));
        return result.getStack().get(0).getString();
    }

    private static String createWCA(
            int stakePer100Token, long totalAmount,
            String[] descriptions, Long[] endTimestamps, int thresholdIndex,
            long coolDownInterval, String identifier
    ) throws Throwable {
        var appLog = invokeWCA("createWCA",
                ContractParameter.hash160(wallet.getDefaultAccount()),
                ContractParameter.integer(stakePer100Token),
                ContractParameter.integer(BigInteger.valueOf(totalAmount)),
                ContractParameter.array(Arrays.asList(descriptions)),
                ContractParameter.array(Arrays.asList(endTimestamps)),
                ContractParameter.integer(thresholdIndex),
                ContractParameter.integer(BigInteger.valueOf(coolDownInterval)),
                ContractParameter.string(identifier));
        return appLog.getExecutions().get(0).getStack().get(0).getString();
    }

    private static void transferCatToken(
            Hash160 to, long amount, String identifier, boolean wait
    ) throws Throwable {
        NeoSendRawTransaction tx = CAT_TOKEN.transferFromDefaultAccount(
                wallet, to, BigInteger.valueOf(amount), ContractParameter.string(identifier)
        ).signers(Signer.calledByEntry(wallet.getDefaultAccount())).sign().send();

        if (tx.hasError())
            throw new Exception(tx.getError().getMessage());

        if (wait)
            Await.waitUntilTransactionIsExecuted(tx.getSendRawTransaction().getHash(), NEOW3J);

        logger.info("Transfer {} {} from {} to {}, tx: {}",
                amount, PublicNetInvoke.CAT_TOKEN.getSymbol(),
                wallet.getDefaultAccount().getAddress(),
                to.toAddress(), tx.getSendRawTransaction().getHash());
    }

    private static String createAndPayWCA(
            int stakePer100Token, long totalAmount,
            String[] descriptions, Long[] endTimestamps, int thresholdIndex,
            long coolDownInterval, String identifier
    ) throws Throwable {
        var result = createWCA(
                stakePer100Token, totalAmount, descriptions, endTimestamps,
                thresholdIndex, coolDownInterval, identifier);

        // pay stake
        transferCatToken(WCA_CONTRACT.getScriptHash(),
                stakePer100Token * totalAmount / 100,
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
