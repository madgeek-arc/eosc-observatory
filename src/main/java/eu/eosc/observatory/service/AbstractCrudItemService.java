package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.catalogue.LoggingUtils;
import gr.athenarc.catalogue.exception.ResourceException;
import gr.athenarc.catalogue.service.AbstractGenericItemService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;

public abstract class AbstractCrudItemService<T extends Identifiable> extends AbstractGenericItemService implements CrudItemService<T> {

    private static final Logger logger = LogManager.getLogger(AbstractCrudItemService.class);

    protected AbstractCrudItemService(ResourceTypeService resourceTypeService,
                                      ResourceService resourceService,
                                      SearchService searchService,
                                      ParserService parserService) {
        super(searchService, resourceService, resourceTypeService, parserService);
    }

    public abstract String createId(T resource);

    public abstract String getResourceType();

    @Override
    public T get(String id) {
        return super.get(getResourceType(), id);
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
}
