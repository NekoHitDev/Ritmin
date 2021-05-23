package info.skyblond.nekohit.neo.pojo;

public class WCAPojo {
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public int remainTokenCount;
    public int endTimestamp;

    public WCAPojo(int stakePer100Token, int maxTokenSoldCount, int remainTokenCount, int endTimestamp) {
        this.stakePer100Token = stakePer100Token;
        this.maxTokenSoldCount = maxTokenSoldCount;
        this.remainTokenCount = remainTokenCount;
        this.endTimestamp = endTimestamp;
    }
}
