package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.model.TimeGrain;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.ResolvedTimeRange;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills gaps in a single time-series result according to its MissingPeriodHandling policy.
 * Assumes the result has exactly one dimension, "period", and that from/to are grain-aligned
 * (e.g. quarter boundaries for QUARTER grain) — multi-dimensional time series (e.g. per-country
 * breakdowns) are out of scope for this first slice.
 */
public class TimeSeriesNormalizer {

    public static final String PERIOD_DIMENSION = "period";

    public IndicatorResult normalize(IndicatorResult result, ResolvedTimeRange timeRange) {
        if (timeRange == null || timeRange.missingPeriods() == MissingPeriodHandling.OMIT) {
            return result;
        }

        Map<String, IndicatorDataPoint> byPeriod = new LinkedHashMap<>();
        for (IndicatorDataPoint point : result.data()) {
            byPeriod.put(String.valueOf(point.dimensions().get(PERIOD_DIMENSION)), point);
        }

        List<IndicatorDataPoint> filled = new ArrayList<>();
        for (String period : generatePeriods(timeRange.grain(), timeRange.from(), timeRange.to())) {
            IndicatorDataPoint existing = byPeriod.get(period);
            if (existing != null) {
                filled.add(existing);
                continue;
            }
            Object value = timeRange.missingPeriods() == MissingPeriodHandling.ZERO_FILL ? 0 : null;
            filled.add(new IndicatorDataPoint(Map.of(PERIOD_DIMENSION, period), value));
        }

        return new IndicatorResult(result.metadata(), result.dimensions(), filled, result.execution());
    }

    private List<String> generatePeriods(TimeGrain grain, LocalDate from, LocalDate to) {
        List<String> periods = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            periods.add(formatPeriod(grain, cursor));
            cursor = advance(grain, cursor);
        }
        return periods;
    }

    private LocalDate advance(TimeGrain grain, LocalDate date) {
        return switch (grain) {
            case DAY -> date.plusDays(1);
            case WEEK -> date.plusWeeks(1);
            case MONTH -> date.plusMonths(1);
            case QUARTER -> date.plusMonths(3);
            case YEAR -> date.plusYears(1);
        };
    }

    private String formatPeriod(TimeGrain grain, LocalDate date) {
        return switch (grain) {
            case DAY -> date.toString();
            case WEEK -> date.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case MONTH -> YearMonth.from(date).toString();
            case QUARTER -> date.getYear() + "-Q" + ((date.getMonthValue() - 1) / 3 + 1);
            case YEAR -> String.valueOf(date.getYear());
        };
    }
}
