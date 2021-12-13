package com.nekohit.neo.domain;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Runtime;

public class ProjectStaticContent {
    /**
     * The ScriptHash of owner.
     */
    public final Hash160 owner;

    /**
     * The description of project.
     */
    public final String description;

    /**
     * Stake rate. Since NeoVM cannot process float number,
     * so we have to limit this to 0.00 (2 decimals)
     */
    public final int stakeRate100;

    /**
     * Total sold token. Represented in fraction.
     */
    public final int maxTokenSoldCount;

    /**
     * The count of milestones. The milestones are stored in separate keys,
     * instead of counting them on the fly, store the count will save gas.
     */
    public final int milestoneCount;

    /**
     * The index of the threshold milestone.
     */
    public final int thresholdIndex;

    /**
     * The millisecond representation of the cooldown interval.
     */
    public final int coolDownInterval;

    /**
     * The timestamp indicate when this project is created.
     */
    public final int creationTimestamp;

    /**
     * The expiry timestamp of the threshold milestone.
     * This field is duplicated, but will save gas if we don't
     * need to read that milestone for it's expire time, considering
     * we need this static content in almost every operation.
     */
    public final int thresholdMilestoneExpireTime;

    /**
     * The expiry timestamp of the last milestone.
     * This field is duplicated, but will save gas if we don't
     * need to read that milestone for it's expire time, considering
     * we need this static content in almost every operation.
     */
    public final int lastMilestoneExpireTime;

    /**
     * If this project will be listed in the result of {@link com.nekohit.neo.contract.WCAContract#advanceQuery(Hash160, Hash160, Hash160, int, int)},
     * but this won't prevent somebody brute force your identifier or
     * read the storage area directly.
     */
    public final boolean bePublic;

    /**
     * Which token is used.
     * */
    public final Hash160 tokenHash;

    // Preserved for future use
    public Object preserved0 = null;
    public Object preserved1 = null;
    public Object preserved2 = null;
    public Object preserved3 = null;
    public Object preserved4 = null;
    public Object preserved5 = null;
    public Object preserved6 = null;
    public Object preserved7 = null;
    public Object preserved8 = null;
    public Object preserved9 = null;

    public ProjectStaticContent(
            Hash160 owner, String description, Hash160 tokenHash,
            int stakeRate100, int maxTokenSoldCount,
            int milestoneCount, int thresholdIndex,
            int coolDownInterval, int thresholdMilestoneExpireTime,
            int lastMilestoneExpireTime, boolean bePublic
    ) {
        this.owner = owner;
        this.description = description;
        this.tokenHash = tokenHash;
        this.stakeRate100 = stakeRate100;
        this.maxTokenSoldCount = maxTokenSoldCount;
        this.milestoneCount = milestoneCount;
        this.thresholdIndex = thresholdIndex;
        this.coolDownInterval = coolDownInterval;
        this.thresholdMilestoneExpireTime = thresholdMilestoneExpireTime;
        this.lastMilestoneExpireTime = lastMilestoneExpireTime;
        this.bePublic = bePublic;

        this.creationTimestamp = Runtime.getTime();
    }


    /**
     * Get total staked token count
     *
     * @return token count in fraction. 1.00 token means 100
     */
    public int getTotalStake() {
        return this.stakeRate100 * this.maxTokenSoldCount / 100;
    }

    /**
     * @return true if the threshold milestone is expired, according to the expiry time.
     */
    public boolean isThresholdExpired() {
        return this.thresholdMilestoneExpireTime <= Runtime.getTime();
    }

    /**
     * @return true if the last milestone is expired, according to the expiry time.
     */
    public boolean isLastExpired() {
        return this.lastMilestoneExpireTime <= Runtime.getTime();
    }
}
