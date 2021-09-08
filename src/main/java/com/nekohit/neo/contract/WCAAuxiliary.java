package com.nekohit.neo.contract;

import com.nekohit.neo.domain.ProjectDynamicContent;
import com.nekohit.neo.domain.ProjectStaticContent;

/**
 * This class contains some helper function specific to WCAContract
 */
public class WCAAuxiliary {

    /**
     * @param staticContent  {@link ProjectStaticContent} of the given project.
     * @param dynamicContent {@link ProjectDynamicContent} of the given project.
     * @return true if the project is ready to finish. (last milestone is finished or expired)
     */
    public static boolean checkIfReadyToFinish(ProjectStaticContent staticContent, ProjectDynamicContent dynamicContent) {
        if (dynamicContent.status != 1) {
            return false;
        }
        return dynamicContent.lastMilestoneFinished || staticContent.isLastExpired();
    }

    /**
     * @param staticContent  {@link ProjectStaticContent} of the given project.
     * @param dynamicContent {@link ProjectDynamicContent} of the given project.
     * @return true if the threshold milestone is finished or expired.
     */
    public static boolean checkIfThresholdMet(ProjectStaticContent staticContent, ProjectDynamicContent dynamicContent) {
        if (dynamicContent.status != 1) {
            return false;
        }
        return dynamicContent.thresholdMilestoneFinished || staticContent.isThresholdExpired();
    }
}
