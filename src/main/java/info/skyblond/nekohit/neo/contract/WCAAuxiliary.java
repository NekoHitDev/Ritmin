package info.skyblond.nekohit.neo.contract;

import info.skyblond.nekohit.neo.domain.WCADynamicContent;
import info.skyblond.nekohit.neo.domain.WCAStaticContent;

/**
 * This class contains some helper function specific to WCAContract
 */
public class WCAAuxiliary {

    public static boolean checkIfReadyToFinish(WCAStaticContent staticContent, WCADynamicContent dynamicContent) {
        if (dynamicContent.status != 1) {
            return false;
        }
        return dynamicContent.lastMilestoneFinished || staticContent.isLastExpired();
    }

    static boolean checkIfThresholdMet(WCAStaticContent staticContent, WCADynamicContent dynamicContent) {
        if (dynamicContent.status != 1) {
            return false;
        }
        return dynamicContent.thresholdMilestoneFinished || staticContent.isThresholdExpired();
    }
}
