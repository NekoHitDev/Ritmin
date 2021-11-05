package com.nekohit.neo.domain;

import com.nekohit.neo.contract.WCAAuxiliary;
import io.neow3j.devpack.contracts.StdLib;

public class ProjectPojo {
    /**
     * The identifier of this project.
     */
    public final String identifier;

    /**
     * The description of this project.
     */
    public final String description;

    /**
     * The Base64 of owner's ScriptHash represented in little endian.
     * Normally we use big endian in Java.
     */
    public final String ownerHashBase64;

    /**
     * The Base64 of token hash represented in little endian..
     * */
    public final String tokenHashBase64;

    /**
     * The timestamp indicate when this project is created.
     */
    public final int creationTimestamp;

    /**
     * Stake rate represented in fraction (2 decimals).
     */
    public final int stakeRate100;

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
    public final ProjectMilestone[] milestones;

    /**
     * The index of the threshold milestone.
     */
    public final int thresholdMilestoneIndex;

    /**
     * Cooldown interval represented in milliseconds.
     */
    public final int coolDownInterval;

    /**
     * The timestamp indicate when the project is updated.
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

    public ProjectPojo(String identifier, ProjectStaticContent staticContent, ProjectDynamicContent dynamicContent, ProjectMilestone[] milestones) {
        // This is a workaround since Hash160 convert to int is too big for
        // StdLib.jsonSerialize, so encoded by Base64 first
        this.identifier = identifier;
        this.description = staticContent.description;
        this.ownerHashBase64 = StdLib.base64Encode(staticContent.owner.toByteString());
        this.tokenHashBase64 = StdLib.base64Encode(staticContent.tokenHash.toByteString());
        this.creationTimestamp = staticContent.creationTimestamp;
        this.stakeRate100 = staticContent.stakeRate100;
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
