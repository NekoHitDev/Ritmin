package com.nekohit.neo.contract;

import com.nekohit.neo.domain.WCADynamicContent;
import com.nekohit.neo.domain.WCAStaticContent;

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

    public static boolean checkIfThresholdMet(WCAStaticContent staticContent, WCADynamicContent dynamicContent) {
        if (dynamicContent.status != 1) {
            return false;
        }
        return dynamicContent.thresholdMilestoneFinished || staticContent.isThresholdExpired();
    }
}
