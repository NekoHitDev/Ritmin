package info.skyblond.nekohit.neo.pojo;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;

public class WCABuyerInfo {
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

    public void addBuyer(Hash160 buyer, int amount) {
        this.buyer.add(buyer);
        this.amount.add(amount);

        this.remainTokenCount -= amount;
        this.totalAmount += amount;
    }

}

