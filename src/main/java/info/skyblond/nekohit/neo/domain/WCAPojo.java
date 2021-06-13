package info.skyblond.nekohit.neo.domain;

import io.neow3j.devpack.List;
import io.neow3j.devpack.contracts.StdLib;

public class WCAPojo {
    public String ownerBase64;
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public int remainTokenCount;
    public int buyerCount;
    public int milestonesCount;
    public List<WCAMilestone> milestones;
    public int thresholdMilestoneIndex;
    public int nextMilestone;
    public boolean stakePaid;

    public WCAPojo(WCABasicInfo basicInfo, WCABuyerInfo buyerInfo) {
        // TODO This is a workaround since Hash160 convert to int is too big for
        // StdLib.jsonSerialize, so encoded by Base64 first
        this.ownerBase64 = StdLib.base64Encode(basicInfo.owner.toByteString());
        this.stakePer100Token = basicInfo.stakePer100Token;
        this.maxTokenSoldCount = basicInfo.maxTokenSoldCount;
        this.remainTokenCount = buyerInfo.remainTokenCount;
        this.buyerCount = buyerInfo.purchases.keys().length;
        this.milestones = basicInfo.milestones;
        this.milestonesCount = basicInfo.milestones.size();
        this.thresholdMilestoneIndex = basicInfo.thresholdIndex;
        this.nextMilestone = basicInfo.nextMilestoneIndex;
        this.stakePaid = basicInfo.paid;
    }
}
