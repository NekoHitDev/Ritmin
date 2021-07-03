package info.skyblond.nekohit.neo.domain;

import io.neow3j.devpack.List;
import io.neow3j.devpack.contracts.StdLib;

public class WCAPojo {
    public String identifier;
    public String description;
    public String ownerHashBase64;
    public int creationTimestamp;
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public int milestonesCount;
    public List<WCAMilestone> milestones;
    public int thresholdMilestoneIndex;
    public int coolDownInterval;
    public int lastUpdateTimestamp;
    public int nextMilestone;
    public int remainTokenCount;
    public int buyerCount;

    public String status;

    public WCAPojo(String identifier, WCABasicInfo basicInfo, List<WCAMilestone> milestones, WCABuyerInfo buyerInfo) {
        // This is a workaround since Hash160 convert to int is too big for
        // StdLib.jsonSerialize, so encoded by Base64 first
        this.identifier = identifier;
        this.description = basicInfo.description;
        this.ownerHashBase64 = StdLib.base64Encode(basicInfo.owner.toByteString());
        this.creationTimestamp = basicInfo.creationTimestamp;
        this.stakePer100Token = basicInfo.stakePer100Token;
        this.maxTokenSoldCount = basicInfo.maxTokenSoldCount;
        this.milestonesCount = basicInfo.milestoneCount;
        this.milestones = milestones;
        this.thresholdMilestoneIndex = basicInfo.thresholdIndex;
        this.coolDownInterval = basicInfo.coolDownInterval;
        this.lastUpdateTimestamp = basicInfo.lastUpdateTime;
        this.nextMilestone = basicInfo.nextMilestoneIndex;
        this.remainTokenCount = buyerInfo.remainTokenCount;
        this.buyerCount = buyerInfo.purchases.keys().length;

        if (!basicInfo.paid) {
            // not paid
            this.status = "PENDING";
        } else if (basicInfo.nextMilestoneIndex == 0 && !milestones.get(0).isExpired()) {
            // paid but not started
            this.status = "OPEN";
        } else if (!basicInfo.finished) {
            // paid, started, but not finished
            this.status = "ACTIVE";
        } else {
            // finished
            this.status = "FINISHED";
        }
    }
}
