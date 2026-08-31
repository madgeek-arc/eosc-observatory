package eu.openaire.observatory.indicator.model;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RenderStyleTest {

    private static final Set<RenderStyle> AGGREGATE_STYLES =
        EnumSet.of(RenderStyle.SCALAR, RenderStyle.LINE_CHART, RenderStyle.STACKED_COLUMN);

    @Test
    void scalarAndLineAndStackedColumnAreNotPerCountry() {
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
