package eu.openaire.observatory.mappers;

import eu.openaire.observatory.domain.NewsItem;
import eu.openaire.observatory.dto.NewsItemDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NewsItemMapper {

    NewsItem toNewsItem(NewsItemDTO source);

    NewsItemDTO toDTO(NewsItem source);
}
