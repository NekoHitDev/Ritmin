package com.nekohit.neo.airdrop;

import com.nekohit.neo.helper.Pair;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.types.Hash160;
import io.neow3j.types.StackItemType;
import io.neow3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.nekohit.neo.airdrop.AirdropUtils.dumpStorageMap;
import static com.nekohit.neo.airdrop.AirdropUtils.stdLibDeserialize;
import static io.neow3j.utils.ArrayUtils.*;

public class WcaContractDumper {
    private final Neow3j neow3j;
    private final long blockIndex;
    private final Hash160 scriptHash;

    public WcaContractDumper(Neow3j neow3j, long blockIndex, String scriptHash) {
        this.neow3j = neow3j;
        this.blockIndex = blockIndex;
        this.scriptHash = new Hash160(scriptHash);
    }

    private HashMap<String, BigInteger> idMap;
    private HashMap<BigInteger, byte[]> scMap;
    private HashMap<BigInteger, byte[]> dcMap;
    // Pair<projectInternalId, sponsorAddr> -> Amount
    private HashMap<Pair<BigInteger, String>, BigInteger> prMap;
    // Pair<projectInternalId, ms index> -> ms obj
    private HashMap<Pair<BigInteger, BigInteger>, List<StackItem>> msMap;

    public void updateStorageMap() throws IOException {
        this.idMap = dumpStorageMap(
                this.neow3j, this.blockIndex, this.scriptHash,
                "ID".getBytes(StandardCharsets.UTF_8),
                key -> new String(key, StandardCharsets.UTF_8),
                value -> Numeric.toBigInt(reverseArray(value)));

        this.scMap = dumpStorageMap(
                this.neow3j, this.blockIndex, this.scriptHash,
                "SC".getBytes(StandardCharsets.UTF_8),
                key -> Numeric.toBigInt(reverseArray(key)),
                it -> it
        );

        this.dcMap = dumpStorageMap(
                this.neow3j, this.blockIndex, this.scriptHash,
                "DC".getBytes(StandardCharsets.UTF_8),
                key -> Numeric.toBigInt(reverseArray(key)),
                it -> it
        );

        this.prMap = dumpStorageMap(
                this.neow3j, this.blockIndex, this.scriptHash,
                "PR".getBytes(StandardCharsets.UTF_8),
                key -> {
                    var id = Numeric.toBigInt(reverseArray(getFirstNBytes(key, key.length - 20)));
                    var addr = new Hash160(reverseArray(getLastNBytes(key, 20))).toAddress();
                    return new Pair<>(id, addr);
                },
                value -> Numeric.toBigInt(reverseArray(value))
        );

        this.msMap = dumpStorageMap(
                this.neow3j, this.blockIndex, this.scriptHash,
                "MS".getBytes(StandardCharsets.UTF_8),
                key -> {
                    var id = Numeric.toBigInt(reverseArray(getFirstNBytes(key, key.length - 20)));
                    var index = Numeric.toBigInt(reverseArray(getLastNBytes(key, 20)));
                    return new Pair<>(id, index);
                },
                value -> {
                    try {
                        return stdLibDeserialize(this.neow3j, value, it -> it);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public static class WcaDumpModel {
        BigInteger projectInternalId;
        String projectIdentifier;
        String creatorAddress;
        String description;
        BigInteger stakeRateInPercent;
        BigInteger maxSoldInFraction;
        BigInteger milestoneCount;
        BigInteger thresholdMilestoneIndex;
        BigInteger cooldownIntervalInMs;
        BigInteger createdTimestampInMs;
        BigInteger thresholdMilestoneExpireTimestampInMs;
        BigInteger lastMilestoneExpireTimestampInMs;
        boolean bePublic;
        Hash160 stakedTokenScriptHash;

        BigInteger remainTokensInFraction;
        BigInteger totalPurchasedInFraction;
        BigInteger buyersCount;
        boolean thresholdMilestoneIsPassed;
        boolean lastMilestoneIsFinished;
        BigInteger lastUpdateTimestampInMs;
        BigInteger finishedMilestonesCount;
        BigInteger nextToBeDoneMilestoneIndex;
        Status status;

        public static enum Status {
            PENDING, ONGOING, FINISHED
        }

        // Address -> Amount
        Map<String, BigInteger> sponsorMap;

        List<MilestoneDumpModel> milestoneList;

        public static class MilestoneDumpModel {
            BigInteger index;
            String title;
            String description;
            BigInteger endTimestampInMs;
            String proofOfWork;
        }
    }

    public List<WcaDumpModel> process() {
        List<WcaDumpModel> result = new LinkedList<>();

        this.idMap.forEach((identifier, projectId) -> {
            try {
                var sc = stdLibDeserialize(this.neow3j, this.scMap.get(projectId), it -> it);
                var dc = stdLibDeserialize(this.neow3j, this.dcMap.get(projectId), it -> it);

                WcaDumpModel w = new WcaDumpModel();
                w.projectInternalId = projectId;
                w.projectIdentifier = identifier;
                w.creatorAddress = sc.get(0).getAddress();
                w.description = sc.get(1).getString();
                w.stakeRateInPercent = sc.get(2).getInteger();
                w.maxSoldInFraction = sc.get(3).getInteger();
                w.milestoneCount = sc.get(4).getInteger();
                w.thresholdMilestoneIndex = sc.get(5).getInteger();
                w.cooldownIntervalInMs = sc.get(6).getInteger();
                w.createdTimestampInMs = sc.get(7).getInteger();
                w.thresholdMilestoneExpireTimestampInMs = sc.get(8).getInteger();
                w.lastMilestoneExpireTimestampInMs = sc.get(9).getInteger();
                w.bePublic = sc.get(10).getBoolean();
                w.stakedTokenScriptHash = Hash160.fromAddress(sc.get(11).getAddress());

                w.remainTokensInFraction = dc.get(0).getInteger();
                w.totalPurchasedInFraction = dc.get(1).getInteger();
                w.buyersCount = dc.get(2).getInteger();
                w.thresholdMilestoneIsPassed = dc.get(3).getBoolean();
                w.lastMilestoneIsFinished = dc.get(4).getBoolean();
                w.lastUpdateTimestampInMs = dc.get(5).getInteger();
                w.finishedMilestonesCount = dc.get(6).getInteger();
                w.nextToBeDoneMilestoneIndex = dc.get(7).getInteger();
                // 8~17 is preserve 0~9
                var status = dc.get(18).getInteger().intValue();
                switch (status) {
                    case 0 -> w.status = WcaDumpModel.Status.PENDING;
                    case 1 -> w.status = WcaDumpModel.Status.ONGOING;
                    case 2 -> w.status = WcaDumpModel.Status.FINISHED;
                }

                w.sponsorMap = new HashMap<>();
                this.prMap.forEach((k, v) -> {
                    if (k.first.equals(projectId)) {
                        w.sponsorMap.put(k.second, v);
                    }
                });

                w.milestoneList = new LinkedList<>();
                this.msMap.forEach((k, ms) -> {
                    if (k.first.equals(projectId)) {
                        WcaDumpModel.MilestoneDumpModel m = new WcaDumpModel.MilestoneDumpModel();
                        m.index = k.second;
                        m.title = ms.get(0).getString();
                        m.description = ms.get(1).getString();
                        m.endTimestampInMs = ms.get(2).getInteger();
                        m.proofOfWork = null;
                        if (ms.get(3).getType() == StackItemType.BYTE_STRING) {
                            m.proofOfWork = ms.get(3).getString();
                        }
                        w.milestoneList.add(m);
                    }
                });

                result.add(w);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });

        return result;
    }

}
