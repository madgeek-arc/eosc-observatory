package eu.openaire.observatory.indicator.openaire;

import java.util.List;

/**
 * Response shape of a GET .../raw?json=... call: one entry per requested series sub-query
 * (outer), one entry per result row (middle, always exactly one row for the bare-count,
 * no-groupBy sub-queries this handler builds), one entry per selected column (inner, always
 * exactly one — the count aggregate).
 */
public record OpenAireRawResponse(List<List<List<String>>> data) {
}
