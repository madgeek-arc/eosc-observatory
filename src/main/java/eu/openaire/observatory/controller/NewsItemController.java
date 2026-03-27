package eu.openaire.observatory.controller;

import eu.openaire.observatory.domain.NewsItem;
import eu.openaire.observatory.dto.NewsItemDTO;
import eu.openaire.observatory.dto.NewsItemPatchRequest;
import eu.openaire.observatory.mappers.NewsItemMapper;
import eu.openaire.observatory.service.NewsItemService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class NewsItemController {

    private final NewsItemService newsItemService;
    private final NewsItemMapper newsItemMapper;

    public NewsItemController(NewsItemService newsItemService, NewsItemMapper newsItemMapper) {
        this.newsItemService = newsItemService;
        this.newsItemMapper = newsItemMapper;
    }

    /*---------------------------*/
    /*        CRUD methods       */
    /*---------------------------*/

    @GetMapping(path = "news/{id}")
    @PostAuthorize("hasAuthority('ADMIN') " +
            "or isStakeholderMember(returnObject.getBody().stakeholderId) " +
            "or isCoordinatorOfStakeholder(returnObject.getBody().stakeholderId) " +
            "or isAdministratorOfStakeholder(returnObject.getBody().stakeholderId)")
    public ResponseEntity<NewsItem> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(newsItemService.get(id), HttpStatus.OK);
    }

    @GetMapping(path = "news")
    @BrowseParameters
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<HighlightedResult<NewsItem>>> getNewsItems(
            @Parameter(hidden = true) @RequestParam
            MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        Browsing<HighlightedResult<NewsItem>> news = newsItemService.getHighlightedResults(filter);
        return new ResponseEntity<>(news, HttpStatus.OK);
    }

    @DeleteMapping("news/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<NewsItem> delete(@PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(newsItemService.delete(id), HttpStatus.OK);
    }

    @DeleteMapping("stakeholders/{stakeholderId}/news/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderMember(#stakeholderId)")
    public ResponseEntity<NewsItem> delete(@PathVariable("stakeholderId") String stakeholderId,
                                           @PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(newsItemService.delete(id), HttpStatus.OK);
    }

    @PostMapping(path = "stakeholders/{stakeholderId}/news")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderMember(#stakeholderId)")
    public ResponseEntity<NewsItem> create(@PathVariable("stakeholderId") String stakeholderId,
                                           @RequestBody NewsItemDTO dto) {
        NewsItem newsItem = newsItemMapper.toNewsItem(dto);
        newsItem.setStakeholderId(stakeholderId);
        return new ResponseEntity<>(newsItemService.add(newsItem), HttpStatus.CREATED);
    }

    @PutMapping("stakeholders/{stakeholderId}/news/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderMember(#stakeholderId)")
    public ResponseEntity<NewsItem> update(@PathVariable("id") String id,
                                           @PathVariable("stakeholderId") String stakeholderId,
                                           @RequestBody NewsItemDTO dto) {
        NewsItem toUpdate = newsItemMapper.toNewsItem(dto);
        return new ResponseEntity<>(newsItemService.update(id, toUpdate), HttpStatus.OK);
    }

    @PatchMapping("stakeholders/{stakeholderId}/news/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<NewsItem> patch(@PathVariable("id") String id,
                                           @PathVariable("stakeholderId") String stakeholderId,
                                           @RequestBody NewsItemPatchRequest request) {
        return new ResponseEntity<>(newsItemService.patch(id, request), HttpStatus.OK);
    }

    @GetMapping(path = "stakeholders/{stakeholderId}/news")
    @BrowseParameters
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderMember(#stakeholderId)")
    public ResponseEntity<Browsing<HighlightedResult<NewsItem>>> getStakeholderNewsItems(
            @PathVariable("stakeholderId") String stakeholderId,
            @Parameter(hidden = true) @RequestParam
            MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);

        filter.addFilter("stakeholderId", stakeholderId);
        Browsing<HighlightedResult<NewsItem>> news = newsItemService.getHighlightedResults(filter);
        return new ResponseEntity<>(news, HttpStatus.OK);
    }

}
