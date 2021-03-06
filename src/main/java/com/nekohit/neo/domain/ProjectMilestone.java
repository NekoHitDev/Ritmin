package com.nekohit.neo.domain;

import io.neow3j.devpack.Runtime;

public class ProjectMilestone {
    /**
     * The title of this milestone
     */
    public String title;

    /**
     * The description of this milestone
     */
    public String description;

    /**
     * The end, or expiry timestamp of this milestone
     */
    public int endTimestamp;

    /**
     * The proof of work, or the result of this milestone.
     * Null if not finished yet.
     */
    public String proofOfWork;

    /**
     * Not used, preserved for future.
     * */
    public int weight = -1;

    // Preserved for future use
    public Object preserved0 = null;
    public Object preserved1 = null;
    public Object preserved2 = null;
    public Object preserved3 = null;
    public Object preserved4 = null;
    public Object preserved5 = null;
    public Object preserved6 = null;
    public Object preserved7 = null;
    public Object preserved8 = null;
    public Object preserved9 = null;

    public ProjectMilestone(String title, String description, int endTimestamp) {
        this.title = title;
        this.description = description;
        this.endTimestamp = endTimestamp;
        proofOfWork = null;
    }

    /**
     * {@link ProjectMilestone#proofOfWork} is considered a proof of milestone.
     * If that is not null, then this milestone is finished.
     *
     * @return true if finished; false if not.
     */
    public boolean isFinished() {
        return proofOfWork != null;
    }

    /**
     * When {@link ProjectMilestone#endTimestamp} is passed, then the milestone
     * is considering as expired, and you can't change it anymore.
     *
     * @return true if this milestone is already expired.
     */
    public boolean isExpired() {
        return endTimestamp <= Runtime.getTime();
    }
}
