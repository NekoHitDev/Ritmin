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
     * so we have to multiply the decimals to convert it into
     * an integer. (aka the fraction representation)
     */
    public final int stakePer100Token;

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
     * If this project will be listed in the result of {@link com.nekohit.neo.contract.WCAContract#advanceQuery(Hash160, Hash160, int, int)},
     * but this won't prevent somebody brute force your identifier or
     * read the storage area directly.
     */
    public final boolean bePublic;

    public ProjectStaticContent(
            Hash160 owner, String description,
            int stakePer100Token, int maxTokenSoldCount,
            int milestoneCount, int thresholdIndex,
            int coolDownInterval, int thresholdMilestoneExpireTime,
            int lastMilestoneExpireTime, boolean bePublic
    ) {
        this.owner = owner;
        this.description = description;
        this.stakePer100Token = stakePer100Token;
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
        return this.stakePer100Token * this.maxTokenSoldCount / 100;
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
