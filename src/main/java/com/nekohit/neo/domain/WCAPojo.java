package com.nekohit.neo.domain;

import com.nekohit.neo.contract.WCAAuxiliary;
import io.neow3j.devpack.contracts.StdLib;

public class WCAPojo {
    public final String identifier;
    public final String description;
    public final String ownerHashBase64;
    public final int creationTimestamp;
    public final int stakePer100Token;
    public final int maxTokenSoldCount;
    public final int milestonesCount;
    public final WCAMilestone[] milestones;
    public final int thresholdMilestoneIndex;
    public final int coolDownInterval;
    public final int lastUpdateTimestamp;
    public final int nextMilestone;
    public final int remainTokenCount;
    public final int buyerCount;
    public final String status;
    public final String stage;

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

        // update status first
        if (dynamicContent.status == 0) {
            this.status = "PENDING";
            this.stage = null;
        } else if (dynamicContent.status == 1) {
            this.status = "ONGOING";
            if (WCAAuxiliary.checkIfReadyToFinish(staticContent, dynamicContent)) {
                this.stage = "Ready-To-Finish";
            } else if (WCAAuxiliary.checkIfThresholdMet(staticContent, dynamicContent)) {
                this.stage = "Active";
            } else {
                this.stage = "Open";
            }
        } else if (dynamicContent.status == 2) {
            this.status = "FINISHED";
            this.stage = null;
        } else {
            this.status = "UNKNOWN";
            this.stage = null;
        }
    }
}
