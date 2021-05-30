package info.skyblond.nekohit.example;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.neow3j.contract.SmartContract;
import io.neow3j.transaction.Signer;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

public class ContractExample {

    private static final Logger log = LoggerFactory.getLogger(ContractExample.class);
    public static void main(String[] args) throws Throwable {
        var contract = new SmartContract(new Hash160("0x3a7b01cf1e62d754578d1065ea30bfffe5b3792b"), Utils.NEOW3J);
        
        // create a WCA, shuold stake 5000_00
        log.info("Create WCA");
        String trueId = createWCA(contract, 1_00, 5000_00, "test_id222");
        log.info("created WCA: {}", trueId);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}", Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
        
        log.info("Pay stake");
        Utils.transferCatToken(Utils.CONTRACT_OWNER_WALLET, contract.getScriptHash(), 5000_00, trueId);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}", Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash())));

        log.info("Prepare test wallet");
        var testWallet = Utils.prepaWallet(5000_00);
        log.info("owner cat balance: {}", Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
        log.info("Test cat balance: {}", Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));

        // buy a WCA
        log.info("Buy WCA");
        Utils.transferCatToken(testWallet, contract.getScriptHash(), 5000_00, trueId);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}", Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
        log.info("Test cat balance: {}", Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));

        // finish WCA
        log.info("Finish WCA");
        finishWCA(contract, trueId, true);
        log.info("WCA info: {}", queryWCAJson(contract, trueId));
        log.info("owner cat balance: {}", Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(Utils.CONTRACT_OWNER_ACCOUNT.getScriptHash())));
        log.info("Test cat balance: {}", Utils.getCatWithDecimals(Utils.CAT_TOKEN.getBalanceOf(testWallet.getDefaultAccount().getScriptHash())));
    }

    /**
     * Create WCA with Contract Owner account
     */
    private static String createWCA(SmartContract contract, int stakePer100Token, int totalAmount, String identifier)
            throws Throwable {
        var tx = contract
                .invokeFunction("createWCA", ContractParameter.hash160(Utils.CONTRACT_OWNER_ACCOUNT),
                        ContractParameter.integer(stakePer100Token), ContractParameter.integer(totalAmount),
                        ContractParameter.integer(BigInteger.valueOf(System.currentTimeMillis() + 180 * 1000)),
                        ContractParameter.string(identifier))
                .signers(Signer.calledByEntry(Utils.CONTRACT_OWNER_ACCOUNT)).wallet(Utils.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();
        AtomicReference<String> trueId = new AtomicReference<>();
        if (response.getError() == null) {
            tx.track().blockingSubscribe(l -> {
                var exe = tx.getApplicationLog().getExecutions().get(0);
                trueId.set(exe.getStack().get(0).getString());
                log.info("createWCA tx: {}", tx.getTxId());
            });
        } else {
            return null;
        }
        log.info("createWCA gas fee: {}", Utils.getGasWithDecimals(tx.getSystemFee() + tx.getNetworkFee()));
        return trueId.get();
    }

    /**
     * Transfer token to contract
     */
    

    private static void finishWCA(SmartContract contract, String identifier, boolean finished) throws Throwable {
        var tx = contract
                .invokeFunction("finishWCA", ContractParameter.string(identifier), ContractParameter.bool(finished))
                .signers(Signer.calledByEntry(Utils.CONTRACT_OWNER_ACCOUNT)).wallet(Utils.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();
        if (response.getError() == null) {
            tx.track().blockingSubscribe(l -> {
                log.info("finishWCA tx: {}", tx.getTxId());
            });
        } else {
            log.error("Error when finishing WCA: {}", response.getError().getMessage());
        }
        log.info("finish WCA gas fee: {}", Utils.getGasWithDecimals(tx.getSystemFee() + tx.getNetworkFee()));
    }

    private static String queryWCAJson(SmartContract contract, String trueId) throws Throwable {
        var tx = contract.invokeFunction("queryWCA", ContractParameter.string(trueId))
                .signers(Signer.calledByEntry(Utils.CONTRACT_OWNER_ACCOUNT)).wallet(Utils.CONTRACT_OWNER_WALLET).sign();
        var response = tx.send();
        AtomicReference<String> result = new AtomicReference<>();
        if (response.getError() == null) {
            tx.track().blockingSubscribe(l -> {
                var exe = tx.getApplicationLog().getExecutions().get(0);
                result.set(exe.getStack().get(0).getString());
                log.info("query WCA tx: {}", tx.getTxId());
            });
        } else {
            log.error("Error when querying WCA: {}", response.getError().getMessage());
            return null;
        }
        log.info("query WCA gas fee: {}", Utils.getGasWithDecimals(tx.getSystemFee() + tx.getNetworkFee()));
        return result.get();
    }
}
