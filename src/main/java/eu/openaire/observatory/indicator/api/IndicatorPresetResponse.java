package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorPreset;

import java.util.List;

public record IndicatorPresetResponse(List<IndicatorPreset> results) {
}
