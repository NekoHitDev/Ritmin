package info.skyblond.nekohit.neo.contract;

import info.skyblond.nekohit.neo.domain.WCADynamicContent;
import info.skyblond.nekohit.neo.domain.WCAStaticContent;

/**
 * This class contains some helper function specific to WCAContract
 */
public class WCAAuxiliary {

    public static boolean checkIfReadyToFinish(WCAStaticContent basicInfo, WCADynamicContent dynamicContent) {
        if (dynamicContent.status != 1) {
            return false;
        }
        return dynamicContent.lastMilestoneFinished || basicInfo.isLastExpired();
    }

    static boolean checkIfThresholdMet(WCAStaticContent basicInfo, WCADynamicContent dynamicContent) {
        if (dynamicContent.status != 1) {
            return false;
        }
        return dynamicContent.thresholdMilestoneFinished || basicInfo.isThresholdExpired();
    }
}
