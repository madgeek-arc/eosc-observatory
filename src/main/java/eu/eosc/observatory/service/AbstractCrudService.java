package eu.eosc.observatory.service;

import eu.eosc.observatory.dto.HistoryDTO;
import eu.eosc.observatory.dto.HistoryEntryDTO;
import eu.openminted.registry.core.domain.*;
import eu.openminted.registry.core.service.*;
import gr.athenarc.catalogue.utils.LoggingUtils;
import gr.athenarc.catalogue.exception.ResourceAlreadyExistsException;
import gr.athenarc.catalogue.exception.ResourceException;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.service.AbstractGenericItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Component
public abstract class AbstractCrudService<T extends Identifiable> extends AbstractGenericItemService implements CrudService<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCrudService.class);

    protected AbstractCrudService(ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  VersionService versionService,
                                  ParserService parserService) {
        super(searchService, resourceService, resourceTypeService, versionService, parserService);
    }

    public abstract String createId(T resource);

    public abstract String getResourceType();

    @Override
    public T get(String id) {
        return super.get(getResourceType(), id);
    }

    @Override
    public T getVersion(String id, String version) {
        Resource resource = getResource(id);
        Version v = versionService.getVersion(resource.getId(), version);
        resource.setPayload(v.getPayload());
        resource.setResourceTypeName(v.getResourceTypeName());
        return (T) parserPool.deserialize(resource, getClassFromResourceType(resource.getResourceTypeName()));
    }

    @Override
    public Resource getResource(String id) {
        return super.resourceService.getResource(super.searchResource(getResourceType(), id, true).getId());
    }

    @Override
    public <T> HistoryDTO getHistory(String resourceId, Function<T, HistoryEntryDTO> transform) {
        Resource resource = this.getResource(resourceId);
        List<Version> versions = resource.getVersions();
        List<HistoryEntryDTO> historyEntryList = new LinkedList<>();

        if (versions == null) {
            T res = (T) parserPool.deserialize(resource, getClassFromResourceType(resource.getResourceTypeName()));
            HistoryEntryDTO latest = transform.apply(res);
            latest.setResourceId(resource.getId());
            latest.setVersion(resource.getVersion());
            historyEntryList.add(latest);
        } else {
            versions.sort(Comparator.comparing(Version::getCreationDate).reversed());

            for (Version version : versions) {
                resource.setPayload(version.getPayload());
                resource.setResourceTypeName(version.getResourceTypeName());
                resource.setResourceType(version.getResourceType());
                T object = (T) parserPool.deserialize(resource, getClassFromResourceType(resource.getResourceTypeName()));
                HistoryEntryDTO entry = transform.apply(object);
                entry.setResourceId(version.getResource().getId());
                entry.setVersion(version.getVersion());
                historyEntryList.add(entry);
            }
        }

        return new HistoryDTO(historyEntryList);
    }

    @Override
    public T restore(String resourceId, String versionId, UnaryOperator<T> transform) {
        Resource resource = this.getResource(resourceId);
        Version versionTo = versionService.getVersion(resource.getId(), versionId);
        resource.setPayload(versionTo.getPayload());
        T object = (T) parserPool.deserialize(resource, getClassFromResourceType(resource.getResourceTypeName()));
        object = transform.apply(object);
        try {
            object = this.update(resource.getResourceTypeName(), (String) object.getId(), object);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
        }
        return object;
    }

    @Override
    public Browsing<T> getAll(FacetFilter filter) {
        filter.setResourceType(getResourceType());
        return super.getResults(filter);
    }

    @Override
    public Browsing<T> getMy(FacetFilter filter) {
        filter.setResourceType(getResourceType());
        return super.getResults(filter);
    }

    @Override
    public T add(T resource) {
        ResourceType resourceType = resourceTypeService.getResourceType(getResourceType());
        Resource res = new Resource();
        res.setResourceTypeName(getResourceType());
        res.setResourceType(resourceType);

        String id = createId(resource);
        Resource existing = searchResource(resourceType.getName(), id, false);
        if (existing != null) {
            throw new ResourceAlreadyExistsException(id, resourceType.getName());
        }
        resource.setId(id);
        String payload = parserPool.serialize(resource, ParserService.ParserServiceTypes.fromString(resourceType.getPayloadType()));
        res.setPayload(payload);
        logger.info(LoggingUtils.addResource(getResourceType(), id, resource));
        resourceService.addResource(res);

        return resource;
    }

    @Override
    public T update(String id, T resource) throws ResourceNotFoundException {
        Object existingId = resource.getId();
        if (!id.equals(existingId)) {
            throw new ResourceException("Resource body id different than path id", HttpStatus.CONFLICT);
        }

        ResourceType resourceType = resourceTypeService.getResourceType(getResourceType());
        Resource res = searchResource(getResourceType(), id, true);
        String payload = parserPool.serialize(resource, ParserService.ParserServiceTypes.fromString(resourceType.getPayloadType()));
        res.setPayload(payload);
        logger.info(LoggingUtils.updateResource(getResourceType(), id, resource));
        resourceService.updateResource(res);

        return resource;
    }

    @Override
    public T delete(String id) throws ResourceNotFoundException {
        return super.delete(getResourceType(), id);
    }

    @Override
    public Set<T> getWithFilter(String key, String value) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter(key, value);
        Browsing<T> results = this.getAll(filter);
        return new HashSet<>(results.getResults());
    }
}
