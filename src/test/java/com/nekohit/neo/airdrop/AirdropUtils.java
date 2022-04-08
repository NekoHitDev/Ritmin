package com.nekohit.neo.airdrop;

import com.nekohit.neo.TestUtils;
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
import io.neow3j.utils.ArrayUtils;
import io.neow3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.neow3j.utils.ArrayUtils.reverseArray;

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

    public static <T, U> HashMap<T, U> dumpStorageMap(
            Neow3j neow3j, long blockIndex, Hash160 contractHash, byte[] prefix,
            Function<byte[], T> keyMapper, Function<byte[], U> valueMapper
    ) throws IOException {
        var result = new HashMap<T, U>();
        var req = neow3j.getStateRoot(blockIndex).send();
        if (req.hasError()) {
            throw new RuntimeException(req.getError().getMessage());
        }
        Hash256 stateRoot = req.getStateRoot().getRootHash();

        String lastKeyHex = "";
        while (true) {
            var resp = neow3j.findStates(
                            stateRoot, contractHash, Numeric.toHexString(prefix), lastKeyHex)
                    .send();
            TestUtils.require(!resp.hasError(), () -> resp.getError().getMessage());
            var results = resp.getStates().getResults();
            if (results.isEmpty()) {
                break;
            }
            lastKeyHex = Numeric.toHexString(Base64.getDecoder().decode(results.get(results.size() - 1).getKey()));

            results.forEach((r) -> {
                var rawKey = Base64.getDecoder().decode(r.getKey());
                // The prefix is removed
                var key = keyMapper.apply(Arrays.copyOfRange(rawKey, prefix.length, rawKey.length));
                var value = valueMapper.apply(Base64.getDecoder().decode(r.getValue()));
                result.put(key, value);
            });
        }

        return result;
    }

    private static final Hash160 STD_LIB_HASH = new Hash160("0xacce6fd80d44e1796aa0c2c625e9e4e0ce39efc0");

    public static <T> T stdLibDeserialize(Neow3j neow3j, byte[] serializedBytes, Function<List<StackItem>, T> arrayToObjMapper) throws IOException {
        var obj = neow3j.invokeFunction(STD_LIB_HASH, "deserialize",
                        List.of(ContractParameter.byteArray(serializedBytes))).send()
                .getInvocationResult().getStack().get(0).getList();
        return arrayToObjMapper.apply(obj);
    }

    /**
     * Pair: Holder address, amount of CAT
     */
    public static HashMap<String, BigInteger> dumpCatHolder(Neow3j neow3j, long blockIndex, Hash160 contractHash) throws IOException {
        byte[] prefix = "asset".getBytes(StandardCharsets.UTF_8);
        return dumpStorageMap(neow3j, blockIndex, contractHash, prefix, key -> {
            var scriptHash = reverseArray(key);
            return new Hash160(scriptHash).toAddress();
        }, value -> Numeric.toBigInt(ArrayUtils.reverseArray(value)));
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
