package info.skyblond.nekohit.neo.domain;

import static info.skyblond.nekohit.neo.helper.Utils.require;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Map;

public class WCABuyerInfo {
    public Map<Hash160, Integer> purchases;
    public int remainTokenCount;
    public int totalAmount;

    public WCABuyerInfo(int remainTokenCount) {
        this.purchases = new Map<>();
        this.remainTokenCount = remainTokenCount;
        this.totalAmount = 0;
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
            int origin = this.purchases.get(buyer);
            this.purchases.put(buyer, origin + amount);
        } else {
            // new buyer
            this.purchases.put(buyer, amount);
        }
        this.remainTokenCount -= amount;
        this.totalAmount += amount;
    }
}
