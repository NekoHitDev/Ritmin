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
    public WCAMilestone[] milestones;
    public int thresholdMilestoneIndex;
    public int coolDownInterval;
    public int lastUpdateTimestamp;
    public int nextMilestone;
    public int remainTokenCount;
    public int buyerCount;

    public String status;

    public WCAPojo(String identifier, WCAStaticContent staticContent, WCADynamicContent dynamicContent, WCAMilestone[] milestones) {
        // This is a workaround since Hash160 convert to int is too big for
        // StdLib.jsonSerialize, so encoded by Base64 first
        this.identifier = identifier;
        this.description = staticContent.description;
        this.ownerHashBase64 = StdLib.base64Encode(staticContent.owner.toByteString());
        this.creationTimestamp = staticContent.creationTimestamp;
        this.stakePer100Token = staticContent.stakePer100Token;
        this.maxTokenSoldCount = staticContent.maxTokenSoldCount;
        this.milestonesCount = staticContent.milestoneCount;
        this.milestones = milestones;
        this.thresholdMilestoneIndex = staticContent.thresholdIndex;
        this.coolDownInterval = staticContent.coolDownInterval;
        this.lastUpdateTimestamp = dynamicContent.lastUpdateTime;
        this.nextMilestone = dynamicContent.nextMilestoneIndex;
        this.remainTokenCount = dynamicContent.remainTokenCount;
        this.buyerCount = dynamicContent.buyerCounter;


        // TODO Status, Stage...

        // update status first
        if (dynamicContent.status == 0) {
            this.status = "PENDING";
        } else if (dynamicContent.status == 1) {
            this.status = "ONGOING";
        } else if (dynamicContent.status == 2) {
            this.status = "FINISHED";
        } else {
            this.status = "UNKNOWN";
        }
    }
}
