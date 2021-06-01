package info.skyblond.nekohit.neo.domain;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;

public class WCABasicInfo {
    public Hash160 owner;
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public List<WCAMilestone> milestones;

    /**
     * Pointer to next ready to finished milestone *INDEX*(i.e. start with 0)
     */
    public int nextMilestoneIndex;
    /**
     * Indicate if the stake is paid
     */
    public boolean paid;

    public WCABasicInfo(Hash160 owner, int stakePer100Token, int maxTokenSoldCount, List<WCAMilestone> milestones) throws Exception {
        this.owner = owner;
        this.stakePer100Token = stakePer100Token;
        this.maxTokenSoldCount = maxTokenSoldCount;
        if (milestones.size() == 0) {
            throw new Exception("You must have at least 1 milestone.");
        }
        this.milestones = milestones;

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
        if (!paid) {
            throw new Exception("You can't buy an unpaid WCA.");
        }
        if (isFinished()) {
            throw new Exception("You can't buy a finished WCA.");
        }
        if (nextMilestoneIndex > 0) {
            throw new Exception("You can't buy a WCA already started.");
        }
    }

    /**
     * Finish a milestone by given index.
     * @param index index(start from 0) of the milestone you want finish
     * @param proofOfWork a link to your work to let buyers review the work
     * @throws Exception throws if reqiurements are not satisfied
     */
    public void finishMilestone(int index, String proofOfWork) throws Exception {
        if (index < nextMilestoneIndex) {
            throw new Exception("You can't finish a missed milestone");
        }
        WCAMilestone ms = milestones.get(index);
        if (ms.isFinished()) {
            throw new Exception("You can't finish a finished milestone");
        }
        if (ms.isExpired()) {
            throw new Exception("You can't finish a expired milestone");
        }
        // not finished nor expired, then we can modify it.
        ms.linkToResult = proofOfWork;
        nextMilestoneIndex = index + 1;
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

    /**
     * Given the number of how many milestones are complete.
     * @return the number
     * @throws Exception
     */
    public int countFinishedMilestone() throws Exception {
        int count = 0;
        // TODO the loop seems like broken: Invalid type for PICKITEM: Any
        for(int i = 0; i < milestones.size(); i++) {
            WCAMilestone ms = milestones.get(i);
            if (ms.isFinished()){
                count++;
            }
        }
        return count;
    }
}