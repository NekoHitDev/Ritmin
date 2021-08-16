package info.skyblond.nekohit.neo.domain;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Runtime;

import static info.skyblond.nekohit.neo.helper.Utils.require;

public class WCABasicInfo {
    public Hash160 owner;
    public String description;
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public int milestoneCount;
    public int thresholdIndex;
    public int coolDownInterval;
    public int creationTimestamp;
    public boolean bePublic;

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
     * Status of this WCA. Note: OPEN might become ACTIVE overtime. <br>
     * 0 - PENDING, the initialized status, only payStake and cancel is allowed.<br>
     * 1 - OPEN, from PENDING, after pay the stake, ready to operate.<br>
     * 2 - ACTIVE, from OPEN, after pass the threshold milestone.<br>
     * 3 - FINISHED, from OPEN or ACTIVE, after creator finish this WCA.<br>
     */
    public int status;

    public WCABasicInfo(
            Hash160 owner, String description,
            int stakePer100Token, int maxTokenSoldCount,
            int milestoneCount, int thresholdIndex, int coolDownInterval, boolean bePublic
    ) throws Exception {
        require(Hash160.isValid(owner), Messages.INVALID_HASH160);
        this.owner = owner;
        require(description != null, Messages.NULL_DESCRIPTION);
        this.description = description;
        require(stakePer100Token > 0, Messages.INVALID_STAKE_RATE);
        require(maxTokenSoldCount > 0, Messages.INVALID_MAX_SELL_AMOUNT);
        this.stakePer100Token = stakePer100Token;
        this.maxTokenSoldCount = maxTokenSoldCount;
        require(milestoneCount > 0, Messages.INVALID_MILESTONES_COUNT);
        this.milestoneCount = milestoneCount;
        if (thresholdIndex >= 0 && thresholdIndex < this.milestoneCount) {
            this.thresholdIndex = thresholdIndex;
        } else {
            throw new Exception(Messages.INVALID_THRESHOLD_INDEX);
        }
        require(coolDownInterval > 0, Messages.INVALID_COOL_DOWN_INTERVAL);
        this.coolDownInterval = coolDownInterval;
        this.bePublic = bePublic;

        this.creationTimestamp = Runtime.getTime();
        this.lastUpdateTime = -1;
        this.finishedCount = 0;
        this.nextMilestoneIndex = 0;
        this.status = 0;
    }


    /**
     * Update the status based on milestones.
     * Mainly: OPEN -> ACTIVE, if the threshold milestone is passed
     */
    public void updateStatus(List<WCAMilestone> milestones) {
        if (this.status == 1) {
            // is open status
            WCAMilestone threshold = milestones.get(this.thresholdIndex);
            if (this.nextMilestoneIndex > this.thresholdIndex ||
                    threshold.isExpired() || threshold.isFinished()) {
                // threshold passed, finished, or expired
                // then set the WCA to ACTIVE
                this.status = 2;
            }
        }
    }

    /**
     * Get total staked token count
     *
     * @return token count in fraction. 1.00 token means 100
     */
    public int getTotalStake() {
        return this.stakePer100Token * this.maxTokenSoldCount / 100;
    }
}
