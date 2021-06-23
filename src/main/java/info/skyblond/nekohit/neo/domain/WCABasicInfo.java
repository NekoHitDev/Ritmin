package info.skyblond.nekohit.neo.domain;

import static info.skyblond.nekohit.neo.helper.Utils.require;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Runtime;

public class WCABasicInfo {
    public Hash160 owner;
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public List<WCAMilestone> milestones;
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

    public WCABasicInfo(
        Hash160 owner, int stakePer100Token, int maxTokenSoldCount, 
        List<WCAMilestone> milestones, int thresholdIndex, int coolDownInterval
    ) throws Exception {
        require(owner.isValid(), "Owner address is not a valid address.");
        this.owner = owner;
        require(stakePer100Token > 0, "The stake amount per 100 token must be positive.");
        require(maxTokenSoldCount > 0, "The max sell token count must be positive.");
        this.stakePer100Token = stakePer100Token;
        this.maxTokenSoldCount = maxTokenSoldCount;
        require(milestones.size() > 0, "You must have at least 1 milestone.");
        this.milestones = milestones;
        if (thresholdIndex >= 0 && thresholdIndex < this.milestones.size()) {
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
    }

    /**
     * Get total staked token count
     * @return token count in fraction. 1.00 token means 100
     */
    public int getTotalStake() {
        return stakePer100Token * maxTokenSoldCount / 100;
    }

    /**
     * Check if this WCA is satisfied the basic needs. 
     * Such as if the stake is paid, the milestone is started. 
     * If pass, then {@link WCABuyerInfo#throwIfNotAvailableToBuy(int)} 
     * @throws Exception if not satified the requirements
     */
    public void throwIfNotAvailableToBuy() throws Exception{
        require(paid, "You can't buy an unpaid WCA.");
        require(!this.isFinished(), "You can't buy a finished WCA.");
        require(nextMilestoneIndex == 0, "You can't buy a WCA already started.");
    }

    /**
     * Finish a milestone by given index.
     * @param index index(start from 0) of the milestone you want finish
     * @param proofOfWork a link to your work to let buyers review the work
     * @throws Exception throws if reqiurements are not satisfied
     */
    public void finishMilestone(int index, String proofOfWork) throws Exception {
        // check cool-down time first
        int currentTime = Runtime.getTime();
        require(lastUpdateTime + coolDownInterval <= currentTime, "Cool down time not met");
        require(index >= nextMilestoneIndex, "You can't finish a missed milestone");
        WCAMilestone ms = milestones.get(index);
        require(!ms.isFinished(), "You can't finish a finished milestone");
        require(!ms.isExpired(), "You can't finish a expired milestone");
        // not finished nor expired, then we can modify it.
        ms.linkToResult = proofOfWork;
        nextMilestoneIndex = index + 1;
        finishedCount++;
        lastUpdateTime = currentTime;
    }

    /**
     * WCA is finished if last milestone is finished, or last milestone 
     * is expired.
     * @return true if WCA is finished, false if not
     */
    public boolean isFinished() {
        WCAMilestone ms = milestones.get(milestones.size() - 1);
        if (ms.isFinished()) {
            return true;
        } else if (ms.isExpired()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean thresholdMet() {
        if (this.nextMilestoneIndex > this.thresholdIndex) {
            // next milestone include the threshold
            return true;
        } else if (this.milestones.get(this.thresholdIndex).isExpired()) {
            // not met the threshold, but threshold ms is expired
            return true;
        } else {
            // really not met the threshold ms
            return false;
        }
    }
}