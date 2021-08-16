package info.skyblond.nekohit.neo.contract;

import info.skyblond.nekohit.neo.domain.Messages;
import info.skyblond.nekohit.neo.domain.WCABasicInfo;
import info.skyblond.nekohit.neo.domain.WCAMilestone;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Runtime;

import static info.skyblond.nekohit.neo.helper.Utils.require;

/**
 * This class contains some helper function specific to WCAContract
 */
public class WCAAuxiliary {

    static void updateMilestone(
            WCABasicInfo basicInfo, List<WCAMilestone> milestones, int index, String proofOfWork
    ) throws Exception {
        // check cool-down time first
        int currentTime = Runtime.getTime();
        require(basicInfo.lastUpdateTime + basicInfo.coolDownInterval <= currentTime, Messages.COOL_DOWN_TIME_NOT_MET);
        require(index >= basicInfo.nextMilestoneIndex, Messages.INVALID_MILESTONE_PASSED);
        WCAMilestone ms = milestones.get(index);
        require(!ms.isFinished(), Messages.INVALID_MILESTONE_FINISHED);
        require(!ms.isExpired(), Messages.INVALID_MILESTONE_EXPIRED);
        // not finished nor expired, then we can modify it.
        require(proofOfWork != null && proofOfWork.length() != 0, Messages.INVALID_PROOF_OF_WORK);
        ms.linkToResult = proofOfWork;
        basicInfo.nextMilestoneIndex = index + 1;
        basicInfo.finishedCount++;
        basicInfo.lastUpdateTime = currentTime;
        // update status if we pass the threshold
        basicInfo.updateStatus(milestones);
    }

    public static boolean checkIfReadyToFinish(List<WCAMilestone> milestones) {
        WCAMilestone ms = milestones.get(milestones.size() - 1);
        return ms.isFinished() || ms.isExpired();
    }

    static boolean checkIfThresholdMet(WCABasicInfo basicInfo, List<WCAMilestone> milestones) {
        basicInfo.updateStatus(milestones);
        return basicInfo.status == 2;
    }
}
