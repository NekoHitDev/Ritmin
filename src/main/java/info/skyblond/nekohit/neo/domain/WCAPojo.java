package info.skyblond.nekohit.neo.domain;

public class WCAPojo {
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public int remainTokenCount;
    public int buyerCount;
    public int endTimestamp;
    public boolean stakePaid;

    public WCAPojo(int stakePer100Token, int maxTokenSoldCount, int remainTokenCount, int buyerCount, int endTimestamp,
            boolean stakePaid) {
        this.stakePer100Token = stakePer100Token;
        this.maxTokenSoldCount = maxTokenSoldCount;
        this.remainTokenCount = remainTokenCount;
        this.buyerCount = buyerCount;
        this.endTimestamp = endTimestamp;
        this.stakePaid = stakePaid;
    }
}
