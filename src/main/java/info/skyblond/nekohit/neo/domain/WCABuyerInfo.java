package info.skyblond.nekohit.neo.domain;

import static info.skyblond.nekohit.neo.helper.Utils.require;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;

public class WCABuyerInfo {
    // Make this a map and rewrite buy function
    public List<Hash160> buyer;
    public List<Integer> amount;
    public int remainTokenCount;
    public int totalAmount;

    public WCABuyerInfo(int remainTokenCount) {
        this.buyer = new List<>();
        this.buyer.clear();
        this.amount = new List<>();
        this.amount.clear();
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
        this.buyer.add(buyer);
        this.amount.add(amount);

        this.remainTokenCount -= amount;
        this.totalAmount += amount;
    }
}
