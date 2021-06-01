package info.skyblond.nekohit.neo.domain;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Runtime;

public class WCAPojo {
    // we can't turn Hash160 into String, there will be invalide char
    // so we convert it to String
    public int owner;
    public int stakePer100Token;
    public int maxTokenSoldCount;
    public int remainTokenCount;
    public int buyerCount;
    public int milestonesCount;
    public List<WCAMilestone> milestones;
    public int nextMilestone;
    public boolean stakePaid;

    public WCAPojo(WCABasicInfo basicInfo, WCABuyerInfo buyerInfo) {
        // TODO still cannot pass Hash160 to user: Operation is not valid due to the current state of the object.
        // this.owner = basicInfo.owner.asByteString().toInteger();
        this.stakePer100Token = basicInfo.stakePer100Token;
        this.maxTokenSoldCount = basicInfo.maxTokenSoldCount;
        this.remainTokenCount = buyerInfo.remainTokenCount;
        this.buyerCount = buyerInfo.buyer.size();
        this.milestones = basicInfo.milestones;
        this.milestonesCount = basicInfo.milestones.size();
        this.nextMilestone = basicInfo.nextMilestoneIndex;
        this.stakePaid = basicInfo.paid;
    }
}
