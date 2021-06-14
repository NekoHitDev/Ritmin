package info.skyblond.nekohit.neo.domain;

import static info.skyblond.nekohit.neo.helper.Utils.require;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Map;
import org.bouncycastle.jce.exception.ExtCertPathBuilderException;

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
        var contain = this.purchases.containsKey(buyer);
        require(remainTokenCount >= amount, "Insufficient token remain in this WCA.");
        require(!contain, "shouldn't contain");
        if (contain) {
            // old buyer, get and add the amount
            Integer origin = this.purchases.get(buyer);
            require(origin != null, "Record exists but amount is null.");
            this.purchases.put(buyer, origin + amount);
        } else {
            // new buyer
            if (true) throw new Exception("!!!");
            this.purchases.put(buyer, amount);
        }
        this.remainTokenCount -= amount;
        this.totalAmount += amount;
    }
}
