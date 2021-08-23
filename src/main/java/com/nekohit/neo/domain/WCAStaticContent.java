package com.nekohit.neo.domain;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Runtime;

public class WCAStaticContent {
    public final Hash160 owner;
    public final String description;
    public final int stakePer100Token;
    public final int maxTokenSoldCount;
    public final int milestoneCount;
    public final int thresholdIndex;
    public final int coolDownInterval;
    public final int creationTimestamp;
    public final int thresholdMilestoneExpireTime;
    public final int lastMilestoneExpireTime;
    public final boolean bePublic;

    public WCAStaticContent(
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

    public boolean isThresholdExpired() {
        return this.thresholdMilestoneExpireTime <= Runtime.getTime();
    }

    public boolean isLastExpired() {
        return this.lastMilestoneExpireTime <= Runtime.getTime();
    }
}
