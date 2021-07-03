package info.skyblond.nekohit.neo.domain;

import io.neow3j.devpack.Runtime;

public class WCAMilestone {
    public String title;
    public String description;
    public int endTimestamp;
    public String linkToResult;

    public WCAMilestone(String title, String description, int endTimestamp) {
        this.title = title;
        this.description = description;
        this.endTimestamp = endTimestamp;
        linkToResult = null;
    }

    /**
     * {@link WCAMilestone#linkToResult} is considered a proof of milestone.
     * If that is not null, then this milestone is finished.
     *
     * @return true if finished; false if not.
     */
    public boolean isFinished() {
        return linkToResult != null;
    }

    /**
     * When {@link WCAMilestone#endTimestamp} is passed, then the milestone
     * is considering as expired, and you can't change it anymore.
     *
     * @return
     */
    public boolean isExpired() {
        return endTimestamp <= Runtime.getTime();
    }
}
