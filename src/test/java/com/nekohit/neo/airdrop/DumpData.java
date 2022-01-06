package com.nekohit.neo.airdrop;

import com.nekohit.neo.TestUtils;
import com.nekohit.neo.helper.Pair;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.types.Hash160;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import static com.nekohit.neo.airdrop.AirdropUtils.*;

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
        // fetch the data
        var catHolderList = dumpCatHolder(CAT_CONTRACT);
        var projectIds = dumpWcaProjects(WCA_CONTRACT, PROJECT_NOT_PENDING);
        var sponsorMap = new HashMap<String, List<Pair<String, BigInteger>>>();
        projectIds.forEach(it -> {
            try {
                sponsorMap.put(it.first, dumpProjectSupporters(WCA_CONTRACT, it.first));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        // check dump integrity
        BigInteger endBlock = NEOW3J.getBlockCount().send().getBlockCount();
        TestUtils.require(startBlock.equals(endBlock), "New block detected");

        // print the result
        catHolderList.forEach(it -> System.out.println("(CAT) " + it.first + ": " + it.second));
        System.out.println("(TOTAL) holders: " + catHolderList.size());
        projectIds.forEach(it -> {
            System.out.println("(PROJECT) " + it.first + ": " + it.second);
            List<Pair<String, BigInteger>> supporters = sponsorMap.get(it.first);
            supporters.forEach(s -> System.out.println("(SPONSOR) " + it.first + ": " + s.first + ": " + s.second));
            System.out.println("(TOTAL) sponsors: " + supporters.size());
        });
        System.out.println("(TOTAL) projects: " + projectIds.size());
        System.out.println("(END) --------------------------------------------------");
    }
}
