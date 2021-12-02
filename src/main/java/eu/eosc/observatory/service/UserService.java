package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import org.springframework.stereotype.Service;

@Service
public class UserService extends AbstractCrudItemService<User> implements CrudItemService<User> {

    protected UserService(ResourceTypeService resourceTypeService, ResourceService resourceService, SearchService searchService, ParserService parserService) {
        super(resourceTypeService, resourceService, searchService, parserService);
    }

    @Override
    public String createId(User resource) {
        return resource.getEmail();
    }

    @Override
    public String getResourceType() {
        return "user";
    }
}
