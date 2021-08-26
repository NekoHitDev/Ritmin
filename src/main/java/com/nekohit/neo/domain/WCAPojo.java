package com.nekohit.neo.domain;

import com.nekohit.neo.contract.WCAAuxiliary;
import io.neow3j.devpack.contracts.StdLib;

public class WCAPojo {
    /**
     * The identifier of this wca.
     */
    public final String identifier;

    /**
     * The description of this wca.
     */
    public final String description;

    /**
     * The Base64 of owner's ScriptHash represented in little endian.
     * Normally we use big endian in Java.
     */
    public final String ownerHashBase64;

    /**
     * The timestamp indicate when this wca is created.
     */
    public final int creationTimestamp;

    /**
     * Stake rate represented in fraction.
     */
    public final int stakePer100Token;

    /**
     * Total sold tokens represented in fraction.
     */
    public final int maxTokenSoldCount;

    /**
     * Total milestone count.
     */
    public final int milestonesCount;

    /**
     * Array of milestones.
     */
    public final WCAMilestone[] milestones;

    /**
     * The index of the threshold milestone.
     */
    public final int thresholdMilestoneIndex;

    /**
     * Cooldown interval represented in milliseconds.
     */
    public final int coolDownInterval;

    /**
     * The timestamp indicate when the wca is updated.
     */
    public final int lastUpdateTimestamp;

    /**
     * Index of the next to-be-done milestone.
     */
    public final int nextMilestone;

    /**
     * How many tokens remained for sale.
     */
    public final int remainTokenCount;

    /**
     * How many addresses have already made the purchase.
     * Refunded addresses are excluded.
     */
    public final int buyerCount;

    /**
     * String representation of status.
     */
    public final String status;

    /**
     * String representation of stage, aka the sub status of a status.
     * For now only ONGOING has stages, rest of status has no stages, so
     * this will be null if the status != ONGOING.
     */
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
