package info.skyblond.nekohit.neo.domain;

public class WCABasicInfo {
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public int endTimestamp;

    public WCABasicInfo(int stakePer100Token, int maxTokenSoldCount, int endTimestamp) {
        this.stakePer100Token = stakePer100Token;
        this.maxTokenSoldCount = maxTokenSoldCount;
        this.endTimestamp = endTimestamp;
    }
}