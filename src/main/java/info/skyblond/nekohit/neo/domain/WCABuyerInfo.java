package info.skyblond.nekohit.neo.domain;

import static info.skyblond.nekohit.neo.helper.Utils.require;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Map;
import info.skyblond.nekohit.neo.helper.Pair;

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
     * @param buyer who is making this purchase
     * @param amount how much does he/her/it want to buy
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
     * 
     * @param basicInfo
     * @param buyer
     * @return Pair(buyer amount, creator amount)
     * @throws Exception
     */
    public Pair<Integer, Integer> partialRefund(WCABasicInfo basicInfo, Hash160 buyer) throws Exception {
        require(this.purchases.containsKey(buyer), "Purchase not found");
        Integer buyerPurchaseAmount = this.purchases.get(buyer);
        require(buyerPurchaseAmount != null, "Purchase found but amount is null");

        int totalMiletones = basicInfo.milestones.size();
        // finished milestone belongs to creator
        int toCreatorAmount = buyerPurchaseAmount * basicInfo.finishedCount / totalMiletones;
        // rest of them goes back to buyer
        int remainAmount = buyerPurchaseAmount - toCreatorAmount;
        // after this, remove this record
        this.purchases.remove(buyer);
        // add to remain token
        this.remainTokenCount += buyerPurchaseAmount;
        // remove from total selled amount
        this.totalPurchasedAmount -= buyerPurchaseAmount;
        return new Pair<Integer,Integer>(remainAmount, toCreatorAmount);
    }

    public int fullRefund(Hash160 buyer) throws Exception {
        require(this.purchases.containsKey(buyer), "Purchase not found");
        Integer amount = this.purchases.get(buyer);
        require(amount != null, "Purchase found but amount is null");
        this.purchases.remove(buyer);
        // add to remain token
        this.remainTokenCount += amount;
        // remove from total selled amount
        this.totalPurchasedAmount -= amount;
        return amount;
    }
}
