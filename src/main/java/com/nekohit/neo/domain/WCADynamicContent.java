package com.nekohit.neo.domain;

import com.nekohit.neo.helper.Pair;

import static com.nekohit.neo.helper.Utils.require;

public class WCADynamicContent {
    /**
     * How many tokens remained for sale.
     */
    public int remainTokenCount;

    /**
     * How many tokens have already been purchased.
     */
    public int totalPurchasedAmount;

    /**
     * How many addresses have been made the purchase.
     * Refund addressed are excluded.
     */
    public int buyerCounter;

    /**
     * If the threshold milestone has been finished by creator/owner.
     */
    public boolean thresholdMilestoneFinished;

    /**
     * If the last milestone has been finished by creator/owner.
     */
    public boolean lastMilestoneFinished;

    /**
     * Indicate when the last milestone is update. Used to calculate the cool-down time.
     */
    public int lastUpdateTime;

    /**
     * Counter for current finished milestone. Updated when finishing a milestone.
     */
    public int finishedMilestoneCount;

    /**
     * Index of the next to-be-done milestone.
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
     * Calculate the partial refund amount.
     *
     * @param staticContent of a given WCA
     * @return Pair(to buyer amount, to creator amount)
     * @throws Exception if partial refund is not available
     */
    public Pair<Integer, Integer> partialRefund(WCAStaticContent staticContent, int buyerPurchaseAmount) throws Exception {
        require(buyerPurchaseAmount != 0, ExceptionMessages.RECORD_NOT_FOUND);

        int totalMilestones = staticContent.milestoneCount;
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

    /**
     * Calculate the full refund amount
     *
     * @param amount the purchase record
     * @return the amount of refund
     * @throws Exception if purchase record is 0 (aka not found)
     */
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
