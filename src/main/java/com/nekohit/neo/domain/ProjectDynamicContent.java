package com.nekohit.neo.domain;

import com.nekohit.neo.helper.Pair;

public class ProjectDynamicContent {
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

    /**
     * Status of this project.<br>
     * 0 - PENDING, the initialized status, only payStake and cancel is allowed.<br>
     * 1 - ONGOING, from PENDING, after pay the stake, ready to operate.<br>
     * 2 - FINISHED, from ONGOING, after creator finish this project.<br>
     */
    public int status;

    public ProjectDynamicContent(int remainTokenCount) {
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
     * @param staticContent of a given project
     * @return Pair(to buyer amount, to creator amount)
     */
    public Pair<Integer, Integer> partialRefund(ProjectStaticContent staticContent, int buyerPurchaseAmount) {
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
     */
    public int fullRefund(int amount) {
        // add to remain token
        this.remainTokenCount += amount;
        // remove from total sold amount
        this.totalPurchasedAmount -= amount;
        this.buyerCounter--;
        return amount;
    }
}
