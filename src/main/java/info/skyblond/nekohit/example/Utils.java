package info.skyblond.nekohit.example;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.neow3j.contract.FungibleToken;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Wallet;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    public static double getGasWithDecimals(long value) {
        return value / Math.pow(10, 8);
    }

    public static double getGasWithDecimals(BigInteger value) {
        return new BigDecimal(value).divide(BigDecimal.TEN.pow(8)).doubleValue();
    }

    public static double getCatWithDecimals(long value) {
        return value / Math.pow(10, 2);
    }

    public static double getCatWithDecimals(BigInteger value) {
        return new BigDecimal(value).divide(BigDecimal.TEN.pow(2)).doubleValue();
    }

    private static void transferToken(FungibleToken token, Wallet wallet, Hash160 to, long amount, String identifier)
            throws Throwable {
        NeoSendRawTransaction tx = token.transferFromDefaultAccount(
            wallet, to, BigInteger.valueOf(amount), ContractParameter.string(identifier)
        ).signers(Signer.calledByEntry(wallet.getDefaultAccount())).sign().send();

        if (tx.hasError()) {
            throw new Exception(tx.getError().getMessage());
        }
        
        Await.waitUntilTransactionIsExecuted(tx.getSendRawTransaction().getHash(), Constants.NEOW3J);
        log.info("Transfer {} tx: {}", token.getName(), tx.getSendRawTransaction().getHash());
    }

    public static void transferCatToken(Wallet wallet, Hash160 to, long amount, String identifier) throws Throwable {
        transferToken(Constants.CAT_TOKEN, wallet, to, amount, identifier);
    }

    public static void transferGasToken(Wallet wallet, Hash160 to, long amount) throws Throwable {
        transferToken(Constants.GAS_TOKEN, wallet, to, amount, null);
    }

    public static Wallet prepaTestWallet(long amount) throws Throwable {
        return prepaWallet(amount, Constants.TEST_USER_WALLET);
    }

    public static Wallet prepaWallet(long amount, Wallet testWallet) throws Throwable {
        transferCatToken(Constants.CONTRACT_OWNER_WALLET, testWallet.getDefaultAccount().getScriptHash(), amount, null);
        transferGasToken(Constants.GENESIS_WALLET, testWallet.getDefaultAccount().getScriptHash(), 1_00000000L);
        return testWallet;
    }
}
