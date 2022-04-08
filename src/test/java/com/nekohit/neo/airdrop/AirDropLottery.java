package com.nekohit.neo.airdrop;

import com.nekohit.neo.helper.Pair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.Hash160;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AirDropLottery {
    private static final Neow3j NEOW3J = Neow3j.build(
            new HttpService("http://10.172.1.11:20332/")
    );
    private static final BigInteger SEED_BLOCK_INDEX = BigInteger.valueOf(942910);
    private static final long SNAPSHOT_BLOCK_INDEX = 1245635;
    private static final List<Hash160> TOKENS = List.of(
            new Hash160("0xcdc17669ce3b7cfa65a29c4941aba14dbff9b12b"), // CAT
            new Hash160("0x48c40d4666f93408be1bef038b6722404d9a4c2a"), // bNEO
            new Hash160("0xd2a4cff31913016155e38e474a2c06d08be276cf"), // GAS
            new Hash160("0xcd48b160c1bbc9d74997b803b9a7ad50a4bef020"), // fUSDT
            new Hash160("0xf0151f528127558851b39c2cd8aa47da7418ab28")  // FLM
    );

    public static void main(String[] args) throws IOException {
        // Project name -> hash
        Map<String, BigInteger> projectTicketMap = new ConcurrentHashMap<>();
        // String -> hash
        Map<String, BigInteger> sponsorTicketMap = new ConcurrentHashMap<>();

        WcaContractDumper dumper = new WcaContractDumper(NEOW3J, SNAPSHOT_BLOCK_INDEX,
                "3d151c524c35ea5cd549323d98e782cfb7403951"); // testnet #2
        dumper.updateStorageMap();
        dumper.process().parallelStream()
                .forEach(w -> {
                    if (TOKENS.contains(w.stakedTokenScriptHash)) {
                        projectTicketMap.put(w.projectIdentifier, AirdropUtils.digest(w.projectIdentifier));
                        w.sponsorMap.forEach((addr, amount) -> {
                            String ticket = w.projectIdentifier + "," + addr + "," + amount.toString();
                            sponsorTicketMap.put(ticket, AirdropUtils.digest(ticket));
                        });
                    }
                });

        BigInteger blockHash = new BigInteger(1, NEOW3J.getBlockHash(SEED_BLOCK_INDEX)
                .send().getBlockHash().toArray());
        System.out.println("Block #" + SEED_BLOCK_INDEX + " hash: " + blockHash.toString(16));

        System.out.println("------------------------- PROJECT -------------------------");
        calculateXorDistance(projectTicketMap, blockHash);
        System.out.println("------------------------- SPONSOR -------------------------");
        calculateXorDistance(sponsorTicketMap, blockHash);
    }

    private static void calculateXorDistance(Map<String, BigInteger> tokenMap, BigInteger blockHash) {
        tokenMap.entrySet().parallelStream()
                // map to: <Ticket, string>
                .map(it -> new Pair<>(it.getValue(), it.getKey()))
                .peek(it -> System.out.println("(TOKEN) " + it.first.toString(16) + "->" + it.second))
                // map to: <<Ticket, XOR>, string>
                .map(it -> new Pair<>(new Pair<>(it.first, it.first.xor(blockHash)), it.second))
                // sort with xor distance
                .sequential()
                .sorted(Comparator.comparing(it -> it.first.second))
                // print the result
                .forEach(it -> System.out.println("(XOR: " + it.first.second + ") " + it.first.first + "->" + it.second));
    }
}
