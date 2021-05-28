package info.skyblond.nekohit.neo.domain;

import io.neow3j.devpack.Hash160;

public class WCABasicInfo {
    public Hash160 owner;
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public int endTimestamp;
    public boolean paid;
    public boolean finished;

    public WCABasicInfo(Hash160 owner, int stakePer100Token, int maxTokenSoldCount, int endTimestamp) {
        this.owner = owner;
        this.stakePer100Token = stakePer100Token;
        this.maxTokenSoldCount = maxTokenSoldCount;
        this.endTimestamp = endTimestamp;
        paid = false;
        finished = false;
    }
}