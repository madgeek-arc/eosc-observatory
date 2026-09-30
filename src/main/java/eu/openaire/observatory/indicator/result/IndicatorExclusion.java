package eu.openaire.observatory.indicator.result;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A value left out of a result for one period because it could not be collected, e.g. the survey
 * question behind it was paused that year. {@code period} is written like the {@code period}
 * dimension of a data point (the year as a string). {@code dimension} and {@code member} name the dimension
 * member that was left out; both are null when the whole value for the period is missing.
 */
public record IndicatorExclusion(
    String period,
    @JsonInclude(JsonInclude.Include.NON_NULL) String dimension,
    @JsonInclude(JsonInclude.Include.NON_NULL) String member,
    String reason
) {
    /** The survey question behind the value is paused in that period's survey. */
    public static final String QUESTION_PAUSED = "QUESTION_PAUSED";

    public static IndicatorExclusion questionPaused(int period, String dimension, String member) {
        return new IndicatorExclusion(String.valueOf(period), dimension, member, QUESTION_PAUSED);
    }

    public static IndicatorExclusion questionPaused(int period) {
        return new IndicatorExclusion(String.valueOf(period), null, null, QUESTION_PAUSED);
    }

    /** Whether this exclusion accounts for a missing point of the group identified by {@code groupKey} in {@code period}. */
    public boolean appliesTo(java.util.Map<String, Object> groupKey, String period) {
        if (!this.period.equals(period)) {
            return false;
        }
        if (dimension == null || !groupKey.containsKey(dimension)) {
            return true;
        }
        return member != null && member.equals(String.valueOf(groupKey.get(dimension)));
    }
}
