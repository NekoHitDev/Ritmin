package info.skyblond.nekohit.neo.domain;

import info.skyblond.nekohit.neo.helper.Pair;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Map;

import static info.skyblond.nekohit.neo.helper.Utils.require;

public class WCABuyerInfo {
    public Map<Hash160, Integer> purchases;
    public int remainTokenCount;
    public int totalPurchasedAmount;

    public WCABuyerInfo(int remainTokenCount) {
        this.purchases = new Map<>();
        this.remainTokenCount = remainTokenCount;
        this.totalPurchasedAmount = 0;
    }

    /**
     * Record buyer's purchase record into list.
     *
     * @param buyer  who is making this purchase
     * @param amount how much does he/her/it wants to buy
     * @throws Exception if remain amount is smaller than buyer's intended amount
     */
    public void recordPurchase(Hash160 buyer, int amount) throws Exception {
        require(remainTokenCount >= amount, "Insufficient token remain in this WCA.");
        if (this.purchases.containsKey(buyer)) {
            // old buyer, get and add the amount
            Integer origin = this.purchases.get(buyer);
            require(origin != null, "Record exists but amount is null.");
            this.purchases.put(buyer, origin + amount);
        } else {
            // new buyer
            this.purchases.put(buyer, amount);
        }
        this.remainTokenCount -= amount;
        this.totalPurchasedAmount += amount;
    }

    /**
     * @param basicInfo of a given WCA
     * @param buyer Hash160 of buyer
     * @return Pair(buyer amount, creator amount)
     * @throws Exception if partial refund is not available
     */
    public Pair<Integer, Integer> partialRefund(WCABasicInfo basicInfo, Hash160 buyer) throws Exception {
        require(this.purchases.containsKey(buyer), ExceptionMessages.RECORD_NOT_FOUND);
        Integer buyerPurchaseAmount = this.purchases.get(buyer);
        require(buyerPurchaseAmount != null, ExceptionMessages.BROKEN_RECORD);

        int totalMilestones = basicInfo.milestoneCount;
        // finished milestone belongs to creator
        int toCreatorAmount = buyerPurchaseAmount * basicInfo.finishedCount / totalMilestones;
        // rest of them goes back to buyer
        int remainAmount = buyerPurchaseAmount - toCreatorAmount;
        // after this, remove this record
        this.purchases.remove(buyer);
        // add to remain token
        this.remainTokenCount += buyerPurchaseAmount;
        // remove from total sold amount
        this.totalPurchasedAmount -= buyerPurchaseAmount;
        return new Pair<>(remainAmount, toCreatorAmount);
    }

    public int fullRefund(Hash160 buyer) throws Exception {
        require(this.purchases.containsKey(buyer), ExceptionMessages.RECORD_NOT_FOUND);
        Integer amount = this.purchases.get(buyer);
        require(amount != null, ExceptionMessages.BROKEN_RECORD);
        this.purchases.remove(buyer);
        // add to remain token
        this.remainTokenCount += amount;
        // remove from total sold amount
        this.totalPurchasedAmount -= amount;
        return amount;
    }
}
