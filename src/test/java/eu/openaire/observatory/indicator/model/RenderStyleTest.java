package eu.openaire.observatory.indicator.model;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RenderStyleTest {

    private static final Set<RenderStyle> AGGREGATE_STYLES = EnumSet.of(
        RenderStyle.SCALAR, RenderStyle.LINE_CHART, RenderStyle.STACKED_COLUMN,
        RenderStyle.BAR_CHART, RenderStyle.MULTI_SERIES_BAR_CHART, RenderStyle.COUNT_BY_AREA_WITH_AVERAGE,
        RenderStyle.COLUMN_CHART_WITH_VALUE_LABELS, RenderStyle.STACKED_BAR_WITH_PROGRESS,
        RenderStyle.PIE_WITH_PROGRESS_BARS, RenderStyle.SHARE_COLUMN_CHART_WITH_VALUE_LABELS);

    @Test
    void euAggregateAndByAreaStylesAreNotPerCountry() {
        for (RenderStyle style : AGGREGATE_STYLES) {
            assertThat(style.perCountryBreakdown()).as(style.name()).isFalse();
        }
    }

    @Test
    void everyOtherStyleIsPerCountry() {
        for (RenderStyle style : RenderStyle.values()) {
            if (AGGREGATE_STYLES.contains(style)) {
                continue;
            }
            assertThat(style.perCountryBreakdown()).as(style.name()).isTrue();
        }
    }

    @Test
    void mapsAndChoroplethsAndMultiSeriesArePerCountry() {
        assertThat(RenderStyle.MAP.perCountryBreakdown()).isTrue();
        assertThat(RenderStyle.CHOROPLETH_MAP_WITH_TOP_5.perCountryBreakdown()).isTrue();
        assertThat(RenderStyle.PROGRESS_BARS.perCountryBreakdown()).isTrue();
        assertThat(RenderStyle.MULTI_SERIES_LINE_CHART.perCountryBreakdown()).isTrue();
        assertThat(RenderStyle.COLUMN_CHART.perCountryBreakdown()).isTrue();
        assertThat(RenderStyle.YES_NO.perCountryBreakdown()).isTrue();
    }
}
