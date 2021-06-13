package info.skyblond.nekohit.neo.domain;

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
     * Add buyer's purchase record into list. Should check constrains with
     * {@link WCABuyerInfo#throwIfNotAvailableToBuy(int)} before.
     * 
     * @param buyer
     * @param amount
     */
    public void addBuyer(Hash160 buyer, int amount) {
        this.buyer.add(buyer);
        this.amount.add(amount);

        this.remainTokenCount -= amount;
        this.totalAmount += amount;
    }

    /**
     * Check if this WCA can be bought.
     * 
     * @param amount the amount of buyer intended to buy
     * @throws Exception throws if not availabale for purchase
     */
    public void throwIfNotAvailableToBuy(int amount) throws Exception {
        if (remainTokenCount < amount) {
            throw new Exception("Insufficient token remain in this WCA.");
        }
    }
}
