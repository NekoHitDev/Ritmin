package com.nekohit.neo.airdrop;

import com.nekohit.neo.helper.Pair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AirDrop {
    private static final Neow3j NEOW3J = Neow3j.build(
//            new HttpService("https://neo3-mainnet.neoline.vip/")
            new HttpService("https://neo3-testnet.neoline.vip/")
    );
    private static final BigInteger BLOCK_INDEX = BigInteger.valueOf(942910);

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        // Address, amount
        Map<String, BigInteger> catHolderMap = new HashMap<>();
        // String, token
        Map<String, BigInteger> projectCreatorTokenMap = new HashMap<>();
        // String token
        Map<String, BigInteger> projectSponsorTokenMap = new HashMap<>();

        String line;
        do {
            line = scanner.nextLine(); // throw exception if scanner EOF
            if (line.startsWith("(CAT) ")) {
                var t = line.split(": ");
                catHolderMap.put(t[0].split(" ")[1], new BigInteger(t[1]));
            } else if (line.startsWith("(PROJECT) ")) {
                projectCreatorTokenMap.put(line, AirdropUtils.digest(line));
            } else if (line.startsWith("(SPONSOR) ")) {
                projectSponsorTokenMap.put(line, AirdropUtils.digest(line));
            }
        } while (!line.isBlank());
        scanner.close();

        BigInteger blockHash = new BigInteger(1, NEOW3J.getBlockHash(BLOCK_INDEX)
                .send().getBlockHash().toArray());
        System.out.println("Block #" + BLOCK_INDEX + " hash: " + blockHash.toString(16));

        System.out.println(catHolderMap);
        System.out.println("------------------------- CREATOR -------------------------");
        calculateXorDistance(projectCreatorTokenMap, blockHash);
        System.out.println("------------------------- SPONSOR -------------------------");
        calculateXorDistance(projectSponsorTokenMap, blockHash);
    }

    private static void calculateXorDistance(Map<String, BigInteger> tokenMap, BigInteger blockHash) {
        tokenMap.entrySet().parallelStream()
                // map to: <Token, raw>
                .map(it -> {
                    System.out.println("(TOKEN) " + it.getValue().toString(16) + "->" + it.getKey());
                    return new Pair<>(it.getValue(), it.getKey());
                })
                // map to: <XOR, raw>
                .map(it -> new Pair<>(it.first.xor(blockHash), it.second))
                // sort with xor distance
                .sequential()
                .sorted(Comparator.comparing(it -> it.first))
                // print the result
                .forEach(it -> System.out.println("(XOR) " + it.first + "->" + it.second));
    }
}
