package info.skyblond.nekohit.neo.domain;

import info.skyblond.nekohit.neo.contract.WCAAuxiliary;
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

        // update status first
        basicInfo.updateStatus(milestones);
        if (basicInfo.status == 0) {
            this.status = "PENDING";
        } else if (basicInfo.status == 1) {
            this.status = "OPEN";
        } else if (basicInfo.status == 2) {
            this.status = "ACTIVE";
        } else if (basicInfo.status == 3) {
            this.status = "FINISHED";
        } else {
            this.status = "UNKNOWN";
        }
    }
}
