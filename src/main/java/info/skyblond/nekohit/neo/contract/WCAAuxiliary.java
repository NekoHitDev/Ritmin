package info.skyblond.nekohit.neo.contract;

import static info.skyblond.nekohit.neo.helper.Utils.require;

import info.skyblond.nekohit.neo.domain.WCABasicInfo;
import info.skyblond.nekohit.neo.domain.WCAMilestone;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Runtime;

/**
 * This class contains some helper function specific to WCAContract.class
 */
public class WCAAuxiliary {
    static void throwIfNotAvailableToBuy(WCABasicInfo basicInfo, List<WCAMilestone> milestones) throws Exception {
        require(basicInfo.paid, "You can't buy an unpaid WCA.");
        require(basicInfo.nextMilestoneIndex == 0, "You can't buy a WCA already started.");
        var fisrtMs = milestones.get(0);
        require(!fisrtMs.isExpired(), "You can't buy a WCA already started.");
    }

    static void updateMilestone(
        WCABasicInfo basicInfo, List<WCAMilestone> milestones, int index, String proofOfWork
    ) throws Exception {
        // check cool-down time first
        int currentTime = Runtime.getTime();
        require(basicInfo.lastUpdateTime + basicInfo.coolDownInterval <= currentTime, "Cool down time not met");
        require(index >= basicInfo.nextMilestoneIndex, "You can't finish a passed milestone");
        WCAMilestone ms = milestones.get(index);
        require(!ms.isFinished(), "You can't finish a finished milestone");
        require(!ms.isExpired(), "You can't finish a expired milestone");
        // not finished nor expired, then we can modify it.
        require(proofOfWork != null && proofOfWork.length() != 0, "Proof of work must be valid.");
        ms.linkToResult = proofOfWork;
        basicInfo.nextMilestoneIndex = index + 1;
        basicInfo.finishedCount++;
        basicInfo.lastUpdateTime = currentTime;
    }

    static boolean checkIfReadyToFinish(List<WCAMilestone> milestones) throws Exception {
        WCAMilestone ms = milestones.get(milestones.size() - 1);
        if (ms.isFinished()) {
            return true;
        } else if (ms.isExpired()) {
            return true;
        } else {
            return false;
        }
    }
    
    static boolean checkIfThresholdMet(WCABasicInfo basicInfo, List<WCAMilestone> milestones) {
        if (basicInfo.nextMilestoneIndex > basicInfo.thresholdIndex) {
            // next milestone include the threshold
            return true;
        } else if (milestones.get(basicInfo.thresholdIndex).isExpired()) {
            // not met the threshold, but threshold ms is expired
            return true;
        } else {
            // really not met the threshold ms
            return false;
        }
    }
}
