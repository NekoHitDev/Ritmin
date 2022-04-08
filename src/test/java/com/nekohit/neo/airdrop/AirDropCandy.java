package com.nekohit.neo.airdrop;

import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.Hash160;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import static com.nekohit.neo.airdrop.AirdropUtils.*;

/**
 * Java 16 has a better performance (overall time).
 * Java 11 has too much stop-the-world gc.
 */
public class AirDropCandy {
    private static final Neow3j NEOW3J = Neow3j.build(
            new HttpService("http://10.172.1.11:20332/")
    );

    // test net
    private static final Hash160 CAT_TOKEN_HASH = new Hash160("f461dff74f454e5016421341f115a2e789eadbd7");
    private static final Hash160 WCA_CONTRACT_HASH = new Hash160("514e4dc6398ba12a8c3a5ed96187d606998c4d93");
    // main net
//    private static final Hash160 CAT_TOKEN_HASH = new Hash160("cdc17669ce3b7cfa65a29c4941aba14dbff9b12b");
//    private static final Hash160 WCA_CONTRACT_HASH = new Hash160("1312460889ef976db3561e7688b077f09d5e98e0");

    private static final File notifyFile = new File("./notification_dump.txt");
    private static final File holderFile = new File("./cat_holder_dump.txt");

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        dumpNotify();
        // dump cat holder
        // TODO set this index
        var blockIndex = getBlockCount(NEOW3J).longValue() - 2;
        //noinspection ResultOfMethodCallIgnored
        holderFile.delete();
        HashMap<String, BigInteger> catHolder = dumpCatHolder(NEOW3J, blockIndex, CAT_TOKEN_HASH);
        PrintWriter writer = new PrintWriter(holderFile);
        catHolder.forEach((addr, value) -> writer.println(addr + ": " + value));
        writer.close();
    }

    public static void dumpNotify() throws IOException, ExecutionException, InterruptedException {
        var startMs = System.currentTimeMillis();

        //noinspection ResultOfMethodCallIgnored
        notifyFile.delete();
        PrintWriter writer = new PrintWriter(notifyFile);
        // Set this to make the CPU on 100% usage, be careful with the TCP connection limit
        // Too much thread will increase the kernel load to switch thread (no need for fiber yet)
        // TODO queue overflow? -> CallerRunsPolicy?
        ExecutorService threadPool = Executors.newFixedThreadPool(500);
        ConcurrentLinkedQueue<CompletableFuture<Void>> linkedList = new ConcurrentLinkedQueue<>();

        // Block starts from 0, so count = a -> height is a-1
        // TODO: Set this index
        var blockCount = getBlockCount(NEOW3J);
        System.out.println("Block count: " + blockCount);

        for (BigInteger i = BigInteger.ZERO; i.compareTo(blockCount) < 0; i = i.add(BigInteger.ONE)) {
            BigInteger currentBlock = i;
            linkedList.add(CompletableFuture.runAsync(() -> dumpTxHashesFromBlock(currentBlock, NEOW3J).forEach(txHash ->
                    linkedList.add(CompletableFuture.runAsync(() -> dumpValidNotification(txHash, NEOW3J, CAT_TOKEN_HASH, WCA_CONTRACT_HASH)
                            .map(notify -> {
                                String common = "0x" + txHash + ": 0x" + notify.getContract()
                                        + ": " + notify.getEventName() + ": ";
                                String detail;
                                List<StackItem> list = notify.getState().getList();
                                switch (notify.getEventName()) {
                                    case "Transfer" -> { // from, to, amount
                                        String fromAddr = null;
                                        if (list.get(0).getValue() != null) {
                                            fromAddr = list.get(0).getAddress();
                                        }
                                        String toAddr = null;
                                        if (list.get(1).getValue() != null) {
                                            toAddr = list.get(1).getAddress();
                                        }
                                        detail = fromAddr + ": " + toAddr + ": " + list.get(2).getInteger();
                                    } // creator, identifier, milestone count
                                    // creator, identifier, amount
                                    case "DeclareProject", "PayStake", "PurchaseProject" -> // buyer, identifier, deal amount
                                            detail = list.get(1).getString()
                                                    + ": " + list.get(0).getAddress()
                                                    + ": " + list.get(2).getInteger();
                                    case "FinishMilestone" -> // identifier, milestone index
                                            detail = list.get(0).getString()
                                                    + ": " + list.get(1).getInteger();
                                    case "FinishProject" -> // identifier
                                            detail = list.get(0).getString();
                                    case "Refund" -> // buyer, identifier, return to buyer amount, return to creator amount
                                            detail = list.get(1).getString()
                                                    + ": " + list.get(0).getAddress()
                                                    + ": " + list.get(2).getInteger()
                                                    + ": " + list.get(3).getInteger();
                                    default -> {
                                        System.err.println("Unknown event name: " + notify.getEventName());
                                        return null; // return null
                                    }
                                }
                                return common + detail;
                            })
                            .filter(Objects::nonNull)
                            .forEach(notify -> {
                                synchronized (writer) {
                                    writer.println(notify);
                                }
                            }), threadPool))), threadPool));
        }

        int i = 0;
        int j = 0;
        for (CompletableFuture<Void> f : linkedList) {
            f.get();
            i++;
            if (i == 1000) { // print a dot every N tasks
                i = 0;
                j++;
                System.out.print(".");
                if (j == 80) { // each line has N dots
                    System.out.println();
                    j = 0;
                }
            }
        }
        System.out.println();

        var endMs = System.currentTimeMillis();
        System.out.println("Total time: " + (endMs - startMs) + "ms");

        threadPool.shutdown();
        boolean terminated;
        do {
            terminated = threadPool.awaitTermination(1, TimeUnit.MINUTES);
        } while (!terminated);
        writer.close();
    }

}
