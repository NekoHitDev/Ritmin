package com.nekohit.neo.domain;

public class ExceptionMessages {
    public static final String INVALID_CALLER =
            "Invalid caller.";

    public static final String INVALID_AMOUNT =
            "Invalid amount. Only positive numbers are allowed.";

    public static final String RECORD_NOT_FOUND =
            "Record not found.";

    public static final String INVALID_STAGE_ACTIVE =
            "Invalid stage: ACTIVE. Only OPEN is allowed.";

    public static final String INVALID_STATUS_ALLOW_PENDING =
            "Invalid status. Only PENDING is allowed.";

    public static final String INVALID_STATUS_ALLOW_ONGOING =
            "Invalid status. Only ONGOING is allowed.";

    public static final String INVALID_STATUS_ALLOW_PENDING_AND_ONGOING =
            "Invalid status. Only PENDING and ONGOING is allowed.";

    public static final String INVALID_STAGE_ALLOW_READY_TO_FINISH =
            "Invalid stage. Only Ready-To-Finished is allowed.";

    public static final String INVALID_STAGE_READY_TO_FINISH =
            "Invalid stage: Ready-To-Finish.";

    public static final String INSUFFICIENT_AMOUNT_REMAIN =
            "Insufficient token remain in this project.";

    public static final String INVALID_HASH160 =
            "Invalid hash160.";

    public static final String INCORRECT_AMOUNT =
            "Amount not correct.";

    public static final String INVALID_PAGE =
            "Invalid page. Only positive numbers are allowed.";

    public static final String INVALID_SIZE =
            "Invalid size. Only positive numbers are allowed.";

    public static final String INVALID_SIGNATURE =
            "Invalid signature.";

    public static final String EMPTY_ID =
            "Empty identifier.";

    public static final String DUPLICATED_ID =
            "Identifier duplicated.";

    public static final String INVALID_MILESTONES_COUNT =
            "Invalid milestones count.";

    public static final String INVALID_TIMESTAMP =
            "Invalid timestamp. Only increasing timestamp is allowed.";

    public static final String EXPIRED_TIMESTAMP =
            "Expired timestamp.";

    public static final String NULL_DESCRIPTION =
            "Invalid description. Only non-null value is allowed.";

    public static final String INVALID_MILESTONE_PASSED =
            "Invalid milestone: Already passed.";

    public static final String INVALID_MILESTONE_FINISHED =
            "Invalid milestone: Already finished.";

    public static final String INVALID_MILESTONE_EXPIRED =
            "Invalid milestone: Already expired.";

    public static final String INVALID_PROOF_OF_WORK =
            "Invalid proof of work. Only non-null and non-blank content allowed.";

    public static final String COOL_DOWN_TIME_NOT_MET =
            "Cool-down time not met";

    public static final String INVALID_STAKE_RATE =
            "Invalid stake amount per 100 token. Only positive numbers are allowed.";

    public static final String INVALID_MAX_SELL_AMOUNT =
            "Invalid max sell token count. Only positive numbers are allowed.";

    public static final String INVALID_THRESHOLD_INDEX =
            "Invalid thresholdIndex: Index out of range.";

    public static final String INVALID_COOL_DOWN_INTERVAL =
            "Invalid cool-down interval. Only positive numbers are allowed.";
}
