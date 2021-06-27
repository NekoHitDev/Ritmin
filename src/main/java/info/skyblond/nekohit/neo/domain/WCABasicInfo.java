package info.skyblond.nekohit.neo.domain;

import static info.skyblond.nekohit.neo.helper.Utils.require;
import io.neow3j.devpack.Hash160;

public class WCABasicInfo {
    public Hash160 owner;
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public int milestoneCount;
    public int thresholdIndex;
    public int coolDownInterval;

    /**
     * Indicate when the last milestone is update. Used to calculate the cool-down time.
     */
    public int lastUpdateTime;

    /**
     * Count current finished ms number. Updated when finishing a milestone.
     */
    public int finishedCount;

    /**
     * Pointer to next ready to finished milestone *INDEX*(i.e. start with 0)
     */
    public int nextMilestoneIndex;
    
    /**
     * Indicate if the stake is paid
     */
    public boolean paid;

    /**
     * If this WCA is accounted and finished.
     */
    public boolean finished;

    public WCABasicInfo(
        Hash160 owner, int stakePer100Token, int maxTokenSoldCount, 
        int milestoneCount, int thresholdIndex, int coolDownInterval
    ) throws Exception {
        require(owner.isValid(), "Owner address is not a valid address.");
        this.owner = owner;
        require(stakePer100Token > 0, "The stake amount per 100 token must be positive.");
        require(maxTokenSoldCount > 0, "The max sell token count must be positive.");
        this.stakePer100Token = stakePer100Token;
        this.maxTokenSoldCount = maxTokenSoldCount;
        require(milestoneCount > 0, "You must have at least 1 milestone.");
        this.milestoneCount = milestoneCount;
        if (thresholdIndex >= 0 && thresholdIndex < this.milestoneCount) {
            this.thresholdIndex = thresholdIndex;
        } else {
            throw new Exception("Invalid value for thresholdIndex");
        }
        require(coolDownInterval >= 0, "Cool down interval must not be negative.");
        this.coolDownInterval = coolDownInterval;

        lastUpdateTime = -1;
        finishedCount = 0;
        nextMilestoneIndex = 0;
        paid = false;
        finished = false;
    }

    /**
     * Get total staked token count
     * @return token count in fraction. 1.00 token means 100
     */
    public int getTotalStake() {
        return stakePer100Token * maxTokenSoldCount / 100;
    }
}