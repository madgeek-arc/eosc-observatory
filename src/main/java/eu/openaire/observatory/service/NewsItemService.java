package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.NewsItem;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.dto.NewsItemDTO;
import eu.openaire.observatory.dto.NewsItemPatchRequest;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.catalogue.service.id.IdGenerator;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class NewsItemService extends AbstractCrudService<NewsItem> implements CrudService<NewsItem> {

    private final IdGenerator<String> idGenerator;

    protected NewsItemService(IdGenerator<String> idGenerator,
                              ResourceTypeService resourceTypeService,
                              ResourceService resourceService,
                              SearchService searchService,
                              VersionService versionService,
                              ParserService parserService,
                              ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.idGenerator = idGenerator;
    }

    @Override
    public String createId(NewsItem resource) {
        return idGenerator.createId("news-", 8);
    }

    @Override
    public String getResourceType() {
        return "news_item";
    }

    @Override
    public NewsItem add(NewsItem resource) {
        resource.setMetadata(new Metadata(SecurityContextHolder.getContext().getAuthentication()));
        resource.setActive(true);
        resource.setStatus(NewsItem.Status.APPROVED); // intentional - may change at some point
        return super.add(resource);
    }

    @Override
    public NewsItem update(String id, NewsItem resource) throws ResourceNotFoundException {
        NewsItem existing = get(id);

        // fill from existing
        resource.setId(id);
        resource.setMetadata(existing.getMetadata());
        resource.setStakeholderId(existing.getStakeholderId());
        resource.setStatus(existing.getStatus());
        resource.setActive(existing.isActive());

        // update metadata
        resource.getMetadata().setModificationDate(new Date());
        resource.getMetadata().setModifiedBy(User.getId(SecurityContextHolder.getContext().getAuthentication()));

        return super.update(id, resource);
    }

    public NewsItem patch(String id, NewsItemPatchRequest request) throws ResourceNotFoundException {
        NewsItem newsItem = get(id);

        if (request.active() != null) newsItem.setActive(request.active());
        if (request.status() != null) newsItem.setStatus(request.status());

        // Approving or deactivating an article is an administrative change with no other record, so
        // stamp who did it. Note modificationDate is deliberately left alone for now — pending a
        // decision on whether a status change counts as a modification of the article itself.
        newsItem.getMetadata().setModifiedBy(User.getId(SecurityContextHolder.getContext().getAuthentication()));

        return super.update(id, newsItem);
    }

    /**
     * Low-level save that bypasses {@link #update}'s administrative protections, for the GDPR purge
     * only.
     *
     * <p>{@code update} deliberately restores the author block, owning stakeholder, status and active
     * flag from the stored copy, and re-stamps {@code modifiedBy} from the security context — so an
     * ordinary edit can never change anything administrative. That protection is load-bearing and
     * stays, which is exactly why erasure cannot go through it: doing so would put the purged user's
     * address back and then overwrite it with the identity of whoever ran the purge.
     *
     * <p>Package-private is the entire access control. {@code UserServiceImpl} sits in this package;
     * controllers do not, so the authorship-forgery hole this opens is unreachable from ordinary
     * code without any extra wiring.
     */
    NewsItem saveScrubbed(String id, NewsItem newsItem) {
        return super.update(id, newsItem);
    }

    @Override
    public <T> Browsing<HighlightedResult<T>> getHighlightedResults(FacetFilter filter) {
        filter.setResourceType(this.getResourceType());
        return super.getHighlightedResults(filter);
    }

    public List<NewsItem> getPublic(String stakeholderId) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(this.getResourceType());
        filter.addFilter("stakeholderId", stakeholderId);
        filter.addFilter("active", "true");
        filter.addFilter("status", "APPROVED");
        filter.addOrderBy("publishDate", "desc");
        Paging<NewsItem> results = this.getResults(filter);
        Date now = new Date();
        return results
                .getResults()
                .stream()
                .filter(r -> r.getPublishDate().before(now) && r.getExpiryDate().after(now))
                .toList();
    }
}
