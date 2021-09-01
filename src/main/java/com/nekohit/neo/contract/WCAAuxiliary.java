package com.nekohit.neo.contract;

import com.nekohit.neo.domain.WCADynamicContent;
import com.nekohit.neo.domain.WCAStaticContent;

/**
 * This class contains some helper function specific to WCAContract
 */
public class WCAAuxiliary {

    /**
     * @param staticContent  {@link WCAStaticContent} of the given wca.
     * @param dynamicContent {@link WCADynamicContent} of the given wca.
     * @return true if the wca is ready to finish. (last milestone is finished or expired)
     */
    public static boolean checkIfReadyToFinish(WCAStaticContent staticContent, WCADynamicContent dynamicContent) {
        if (dynamicContent.status != 1) {
            return false;
        }
        return dynamicContent.lastMilestoneFinished || staticContent.isLastExpired();
    }

    /**
     * @param staticContent  {@link WCAStaticContent} of the given wca.
     * @param dynamicContent {@link WCADynamicContent} of the given wca.
     * @return true if the threshold milestone is finished or expired.
     */
    public static boolean checkIfThresholdMet(WCAStaticContent staticContent, WCADynamicContent dynamicContent) {
        if (dynamicContent.status != 1) {
            return false;
        }
        return dynamicContent.thresholdMilestoneFinished || staticContent.isThresholdExpired();
    }
}
