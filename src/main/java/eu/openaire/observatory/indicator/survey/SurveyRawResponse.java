package eu.openaire.observatory.indicator.survey;

import java.util.List;

/**
 * Response shape of the survey stats-tool's GET .../raw?json=... call, confirmed live against
 * http://dl123.madgik.di.uoa.gr:8080/stats-api (profile "eosc-obs") — a different envelope from
 * OpenAIRE's stats-tool (OpenAireRawResponse). One dataset per requested series
 * (this client always requests exactly one); each row is either [countryCode, value] for a
 * per-country question or [value] alone for a ".sum"-suffixed aggregate query. Row elements can
 * be a real JSON null, the literal string "null", or an empty string — all three were observed
 * live and all mean "no answer".
 */
public record SurveyRawResponse(List<Dataset> datasets) {

    public record Dataset(Series series) {
    }

    public record Series(List<RowWrapper> result) {
    }

    public record RowWrapper(List<String> row) {
    }
}
