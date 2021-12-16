package eu.eosc.observatory.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.ResourceTypeService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Date;

@Configuration
public class ResourceTypeInit {

    private static final Logger logger = LogManager.getLogger(ResourceTypeInit.class);

    private final ResourceLoader resourceLoader;
    private final ResourceTypeService resourceTypeService;
    private final ObjectMapper mapper = new ObjectMapper();


    @Autowired
    public ResourceTypeInit(ResourceLoader resourceLoader, ResourceTypeService resourceTypeService) {
        this.resourceLoader = resourceLoader;
        this.resourceTypeService = resourceTypeService;
    }

    @PostConstruct
    void addResourceTypes() throws IOException {
        Resource[] resources = loadResources("classpath:resourceTypes/*.json");
        for (Resource resource : resources) {
            try {
                addResourceTypeFromFile(resource);
            } catch (IOException e) {
                logger.error(String.format("Could not add Resource Type from file [filename=%s]", resource.getFilename()), e);
            }
        }
    }

    private Resource[] loadResources(String pattern) throws IOException {
        return ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources(pattern);
    }

    private void addResourceTypeFromFile(Resource resource) throws IOException {
        ResourceType resourceType = mapper.readValue(resource.getInputStream(), ResourceType.class);
        if (resourceTypeService.getResourceType(resourceType.getName()) == null) {
            logger.info(String.format("Adding [resourceType=%s]", resourceType.getName()));
            resourceType.setCreationDate(new Date());
            resourceType.setModificationDate(new Date());
            resourceTypeService.addResourceType(resourceType);
        } else {
            logger.info(String.format("Found [resourceType=%s]", resourceType.getName()));
        }
    }
}
