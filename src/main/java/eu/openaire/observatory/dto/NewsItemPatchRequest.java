package eu.openaire.observatory.dto;

import eu.openaire.observatory.domain.NewsItem;

public record NewsItemPatchRequest(
        Boolean active,
        NewsItem.Status status
) {
}
