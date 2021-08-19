package info.skyblond.nekohit.neo.domain;

import info.skyblond.nekohit.neo.helper.Pair;

import static info.skyblond.nekohit.neo.helper.Utils.require;

public class WCADynamicContent {
    public int remainTokenCount;
    public int totalPurchasedAmount;
    public int buyerCounter;
    public boolean thresholdMilestoneFinished;
    public boolean lastMilestoneFinished;

    /**
     * Indicate when the last milestone is update. Used to calculate the cool-down time.
     */
    public int lastUpdateTime;

    /**
     * Count current finished ms number. Updated when finishing a milestone.
     */
    public int finishedMilestoneCount;

    /**
     * Pointer to next ready to finished milestone *INDEX*(i.e. start with 0)
     */
    public int nextMilestoneIndex;

    /**
     * Status of this WCA.<br>
     * 0 - PENDING, the initialized status, only payStake and cancel is allowed.<br>
     * 1 - ONGOING, from PENDING, after pay the stake, ready to operate.<br>
     * 2 - FINISHED, from ONGOING, after creator finish this WCA.<br>
     */
    public int status;

    public WCADynamicContent(int remainTokenCount) {
        this.remainTokenCount = remainTokenCount;
        this.totalPurchasedAmount = 0;
        this.buyerCounter = 0;
        this.thresholdMilestoneFinished = false;
        this.lastMilestoneFinished = false;
        this.lastUpdateTime = -1;
        this.finishedMilestoneCount = 0;
        this.nextMilestoneIndex = 0;
        this.status = 0;
    }

    /**
     * @param basicInfo of a given WCA
     * @return Pair(buyer amount, creator amount)
     * @throws Exception if partial refund is not available
     */
    public Pair<Integer, Integer> partialRefund(WCAStaticContent basicInfo, int buyerPurchaseAmount) throws Exception {
        require(buyerPurchaseAmount != 0, ExceptionMessages.RECORD_NOT_FOUND);

        int totalMilestones = basicInfo.milestoneCount;
        // finished milestone belongs to creator
        int toCreatorAmount = buyerPurchaseAmount * this.finishedMilestoneCount / totalMilestones;
        // rest of them goes back to buyer
        int remainAmount = buyerPurchaseAmount - toCreatorAmount;
        // add to remain token
        this.remainTokenCount += buyerPurchaseAmount;
        // remove from total sold amount
        this.totalPurchasedAmount -= buyerPurchaseAmount;
        this.buyerCounter--;
        return new Pair<>(remainAmount, toCreatorAmount);
    }

    public int fullRefund(int amount) throws Exception {
        require(amount != 0, ExceptionMessages.RECORD_NOT_FOUND);
        // add to remain token
        this.remainTokenCount += amount;
        // remove from total sold amount
        this.totalPurchasedAmount -= amount;
        this.buyerCounter--;
        return amount;
    }
}
