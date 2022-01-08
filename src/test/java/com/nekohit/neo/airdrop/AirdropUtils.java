package com.nekohit.neo.airdrop;

import com.nekohit.neo.TestUtils;
import com.nekohit.neo.helper.Pair;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoApplicationLog.Execution.Notification;
import io.neow3j.protocol.core.response.NeoGetApplicationLog;
import io.neow3j.protocol.core.response.NeoGetBlock;
import io.neow3j.protocol.core.response.Transaction;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.types.NeoVMStateType;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static com.nekohit.neo.TestUtils.reverse;

public class AirdropUtils {
    private static MessageDigest newMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA3-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static BigInteger digest(String str) {
        return digest(str.getBytes(StandardCharsets.UTF_8));
    }

    public static BigInteger digest(byte[] data) {
        var d = newMessageDigest();
        var output = d.digest(data);
        // return as a positive (unsigned) bit integer
        return new BigInteger(1, output);
    }

    public interface WcaProjectFilter {
        boolean filter(
                String projectId, String ownerAddress, Hash160 tokenScriptHash,
                Timestamp createdTime, BigInteger stakeRate100, BigInteger maxSell,
                BigInteger msCount, BigInteger coolDownMs, Timestamp lastUpdateTimestamp,
                BigInteger remainToken, BigInteger buyerCount, String status, String stage
        );
    }

    public static final WcaProjectFilter PROJECT_NOT_PENDING =
            (projectId, ownerAddress, tokenScriptHash, createdTime, stakeRate100,
             maxSell, msCount, coolDownMs, lastUpdateTimestamp, remainToken,
             buyerCount, status, stage) -> !status.equals("PENDING");

    /**
     * Pair: Project id and creator address
     */
    public static List<Pair<String, String>> dumpWcaProjects(
            SmartContract wcaContract, WcaProjectFilter filter) throws IOException {

        var result = new LinkedList<Pair<String, String>>();
        for (int page = 1; true; page++) {
            var currentPage = wcaContract
                    .callInvokeFunction(
                            "advanceQueryProto",
                            List.of(
                                    ContractParameter.hash160(Hash160.ZERO),
                                    ContractParameter.hash160(Hash160.ZERO),
                                    ContractParameter.hash160(Hash160.ZERO),
                                    ContractParameter.integer(page),
                                    ContractParameter.integer(20)
                            )
                    )
                    .getInvocationResult()
                    .getStack()
                    .get(0)
                    .getList();
            if (currentPage.size() == 0) {
                break;
            }
            currentPage.forEach(it -> {
                List<StackItem> obj = it.getList();
                String projectId = obj.get(0).getString();
                String owner = new Hash160(reverse(Base64.getDecoder().decode(obj.get(2).getString()))).toAddress();
                Hash160 token = new Hash160(reverse(Base64.getDecoder().decode(obj.get(3).getString())));
                Timestamp createdTime = new Timestamp(obj.get(4).getInteger().longValue());
                BigInteger rate100 = obj.get(5).getInteger();
                BigInteger maxSell = obj.get(6).getInteger();
                BigInteger msCount = obj.get(7).getInteger();
                BigInteger coolDownMs = obj.get(10).getInteger();
                Timestamp lastUpdateTimestamp = new Timestamp(obj.get(11).getInteger().longValue());
                BigInteger remainToken = obj.get(13).getInteger();
                BigInteger buyerCount = obj.get(14).getInteger();
                String status = obj.get(15).getString();
                String stage = null;
                if (obj.get(16).getValue() != null) {
                    stage = obj.get(16).getString();
                }

                if (filter.filter(projectId, owner, token, createdTime, rate100,
                        maxSell, msCount, coolDownMs, lastUpdateTimestamp, remainToken,
                        buyerCount, status, stage)) {
                    // passed the filter
                    result.add(new Pair<>(projectId, owner));
                }
            });
        }
        return result;
    }

    /**
     * Pair: Holder address, amount of CAT
     */
    public static List<Pair<String, BigInteger>> dumpCatHolder(SmartContract catContract) throws IOException {
        var result = new LinkedList<Pair<String, BigInteger>>();

        for (int page = 1; true; page++) {
            var currentPage = catContract
                    .callInvokeFunction(
                            "dumpHolder",
                            List.of(
                                    ContractParameter.integer(page),
                                    ContractParameter.integer(20)
                            )
                    )
                    .getInvocationResult()
                    .getStack()
                    .get(0)
                    .getList();
            if (currentPage.size() == 0) {
                break;
            }
            currentPage.forEach(it -> {
                List<StackItem> pair = it.getList();
                result.add(new Pair<>(pair.get(0).getAddress(), pair.get(1).getInteger()));
            });
        }
        return result;
    }

    /**
     * Pair: Supporter address, supported amount
     */
    public static List<Pair<String, BigInteger>> dumpProjectSupporters(SmartContract wcaContract, String projectId) throws IOException {
        var result = new LinkedList<Pair<String, BigInteger>>();

        for (int page = 1; true; page++) {
            var currentPage = wcaContract
                    .callInvokeFunction(
                            "dumpPurchaseRecord",
                            List.of(
                                    ContractParameter.string(projectId),
                                    ContractParameter.integer(page),
                                    ContractParameter.integer(20)
                            )
                    )
                    .getInvocationResult()
                    .getStack()
                    .get(0)
                    .getList();
            if (currentPage.size() == 0) {
                break;
            }
            currentPage.forEach(it -> {
                List<StackItem> pair = it.getList();
                var amount = pair.get(1).getInteger();
                if (!amount.equals(BigInteger.ZERO)) {
                    result.add(new Pair<>(pair.get(0).getAddress(), amount));
                }
            });
        }
        return result;
    }

    public static BigInteger getBlockCount(Neow3j neow3j) {
        try {
            return neow3j.getBlockCount().send().getBlockCount();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return a parallel stream represent the tx hashes in the block
     */
    public static Stream<Hash256> dumpTxHashesFromBlock(BigInteger blockIndex, Neow3j neow3j) {
        NeoGetBlock bResp;
        try {
            bResp = neow3j.getBlock(blockIndex, true).send();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        TestUtils.require(!bResp.hasError(), () -> bResp.getError().getMessage() + "#" + blockIndex);
        return bResp.getBlock().getTransactions().parallelStream()
                .map(Transaction::getHash);
    }

    /**
     * Return a parallel stream represent the tx hashes in the block
     */
    public static Stream<Notification> dumpValidNotification(
            Hash256 txHash, Neow3j neow3j,
            Hash160 catTokenHash, Hash160 wcaContractHash
    ) {
        var req = neow3j.getApplicationLog(txHash);
        NeoGetApplicationLog resp;
        try {
            resp = req.send();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        TestUtils.require(!resp.hasError(), () -> resp.getError().getMessage());
        var applicationLog = resp.getApplicationLog();

        return applicationLog.getExecutions()
                .parallelStream()
                .filter(it -> it.getState() == NeoVMStateType.HALT)
                .flatMap(it -> it.getNotifications().parallelStream())
                .filter(it -> it.getContract().equals(catTokenHash)
                        || it.getContract().equals(wcaContractHash));
    }
}
