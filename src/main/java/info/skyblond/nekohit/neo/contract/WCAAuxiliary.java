package info.skyblond.nekohit.neo.contract;

import info.skyblond.nekohit.neo.domain.ExceptionMessages;
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
        require(basicInfo.lastUpdateTime + basicInfo.coolDownInterval <= currentTime, ExceptionMessages.COOL_DOWN_TIME_NOT_MET);
        require(index >= basicInfo.nextMilestoneIndex, ExceptionMessages.INVALID_MILESTONE_PASSED);
        WCAMilestone ms = milestones.get(index);
        require(!ms.isFinished(), ExceptionMessages.INVALID_MILESTONE_FINISHED);
        require(!ms.isExpired(), ExceptionMessages.INVALID_MILESTONE_EXPIRED);
        // not finished nor expired, then we can modify it.
        require(proofOfWork != null && proofOfWork.length() != 0, ExceptionMessages.INVALID_PROOF_OF_WORK);
        ms.linkToResult = proofOfWork;
        basicInfo.nextMilestoneIndex = index + 1;
        basicInfo.finishedCount++;
        basicInfo.lastUpdateTime = currentTime;
        // update status if we pass the threshold
        updateStatus(basicInfo, milestones);
    }

    public static boolean checkIfReadyToFinish(List<WCAMilestone> milestones) {
        WCAMilestone ms = milestones.get(milestones.size() - 1);
        return ms.isFinished() || ms.isExpired();
    }

    static boolean checkIfThresholdMet(WCABasicInfo basicInfo, List<WCAMilestone> milestones) {
        updateStatus(basicInfo, milestones);
        return basicInfo.status == 2;
    }

    /**
     * Update the status based on milestones.
     * Mainly: OPEN -> ACTIVE, if the threshold milestone is passed
     */
    public static void updateStatus(WCABasicInfo basicInfo, List<WCAMilestone> milestones) {
        if (basicInfo.status == 1) {
            // is open status
            WCAMilestone threshold = milestones.get(basicInfo.thresholdIndex);
            if (basicInfo.nextMilestoneIndex > basicInfo.thresholdIndex ||
                    threshold.isExpired() || threshold.isFinished()) {
                // threshold passed, finished, or expired
                // then set the WCA to ACTIVE
                basicInfo.status = 2;
            }
        }
    }
}
