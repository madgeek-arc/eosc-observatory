package eu.openaire.observatory.indicator.handler;

/**
 * Marker for handlers that genuinely fetch every period in a requested time range (as opposed to
 * e.g. SurveyStatsToolIndicatorQueryHandler, which collapses any range down to a single year — see
 * its class Javadoc), and so can support comparisonMode requests, which need real values at two or
 * more distinct periods to diff.
 */
public interface SupportsComparisonQueries {
}
