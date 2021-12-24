package com.nekohit.neo.testnet;

import com.nekohit.neo.TestUtils;
import com.nekohit.neo.helper.Pair;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

public class DumpData {
    private static final Neow3j NEOW3J = Neow3j.build(
            new HttpService("https://neo3-testnet.neoline.vip/")
    );

    private static final SmartContract CAT_CONTRACT = new SmartContract(new Hash160("0xf461dff74f454e5016421341f115a2e789eadbd7"), NEOW3J);

    // or 0x3d151c524c35ea5cd549323d98e782cfb7403951 for dev version
    private static final SmartContract WCA_CONTRACT = new SmartContract(new Hash160("0x514e4dc6398ba12a8c3a5ed96187d606998c4d93"), NEOW3J);

    public static void main(String[] args) throws IOException {
        BigInteger startBlock = NEOW3J.getBlockCount().send().getBlockCount();
        System.out.println("Last block number: " + startBlock);
        System.out.println("CAT holders:");
        var catHolderList = dumpCatHolder();
        catHolderList.forEach(it -> System.out.println(it.first + ": "
                + it.second.divide(BigInteger.valueOf(100)) + "."
                + it.second.remainder(BigInteger.valueOf(100)) + " CAT"));
        System.out.println("Total holders: " + catHolderList.size());

        System.out.print("\nWCA projects:");
        var projectIds = dumpWcaProjects();
        projectIds.forEach(it -> {
            System.out.println("\nProject: " + it.first + ", by " + it.second);
            try {
                System.out.println("Sponsored by:");
                List<Pair<String, BigInteger>> supporters = dumpProjectSupporters(it.first);
                supporters.forEach(s -> System.out.println("\tAddress: " + s.first + ": " + s.second));
                System.out.println("\tTotal sponsors: " + supporters.size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("Total projects: " + projectIds.size());

        BigInteger endBlock = NEOW3J.getBlockCount().send().getBlockCount();
        TestUtils.require(startBlock.equals(endBlock), "New block detected");
    }

    private static List<Pair<String, BigInteger>> dumpCatHolder() throws IOException {
        var result = new LinkedList<Pair<String, BigInteger>>();

        for (int page = 1; true; page++) {
            var currentPage = CAT_CONTRACT
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

    // Project id and creator address
    @SuppressWarnings("unused")
    private static List<Pair<String, String>> dumpWcaProjects() throws IOException {
        var result = new LinkedList<Pair<String, String>>();
        for (int page = 1; true; page++) {
            var currentPage = WCA_CONTRACT
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
                int rate100 = obj.get(5).getInteger().intValue();
                BigInteger maxSell = obj.get(6).getInteger();
                int msCount = obj.get(7).getInteger().intValue();
                long coolDownMs = obj.get(10).getInteger().longValue();
                Timestamp lastUpdateTimestamp = new Timestamp(obj.get(11).getInteger().longValue());
                BigInteger remainToken = obj.get(13).getInteger();
                long buyerCount = obj.get(14).getInteger().longValue();
                String status = obj.get(15).getString();
                String stage = null;
                if (obj.get(16).getValue() != null) {
                    stage = obj.get(16).getString();
                }

                System.out.println("Project: " + projectId);
//                System.out.println("Owner: " + owner);
//                System.out.println("Token: 0x" + token);
//                System.out.println("Created time: " + createdTime);
//                System.out.println("Stake rate: " + rate100 + "%");
//                System.out.println("Max sell in fraction: " + maxSell.toString());
//                System.out.println("Remain token in fraction: " + remainToken.toString());
//                System.out.println("Buyer count: " + buyerCount);
//                System.out.println("Milestones count: " + msCount);
//                System.out.println("Cooldown interval(ms): " + coolDownMs);
//                System.out.println("Last update: " + lastUpdateTimestamp);
//                System.out.println("Status: " + status);
//                System.out.println("Stage: " + stage);
//                System.out.println(obj);
//                System.out.println();

                if (status.equals("PENDING")) {
                    return; // skip pending
                }

                result.add(new Pair<>(projectId, owner));
            });
        }

        return result;
    }

    private static List<Pair<String, BigInteger>> dumpProjectSupporters(String projectId) throws IOException {
        var result = new LinkedList<Pair<String, BigInteger>>();

        for (int page = 1; true; page++) {
            var currentPage = WCA_CONTRACT
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
                result.add(new Pair<>(pair.get(0).getAddress(), pair.get(1).getInteger()));
            });
        }
        return result;
    }

    /**
     * reverse the content in buffer in place, return the buffer.
     */
    private static byte[] reverse(byte[] buffer) {
        int size = buffer.length;
        byte temp;
        for (int i = 0; i < size / 2; i++) {
            temp = buffer[i];
            buffer[i] = buffer[size - i - 1];
            buffer[size - i - 1] = temp;
        }
        return buffer;
    }
}
