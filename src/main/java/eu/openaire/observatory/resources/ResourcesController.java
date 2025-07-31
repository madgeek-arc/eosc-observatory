package eu.openaire.observatory.resources;

import eu.openaire.observatory.resources.model.Document;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class ResourcesController {

    private final GenericResourceService genericResourceService;

    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceTypeService resourceTypeService;

    public ResourcesController(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
    }

    @GetMapping
    @BrowseParameters
    public ResponseEntity<Paging<Document>> getDocuments(@Parameter(hidden = true)
                                                       @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType("document");
        filter.addFilter("status", "generated");
        Paging<Document> docs = genericResourceService.getResults(filter);
        return new ResponseEntity<>(docs, HttpStatus.OK);
    }

    @GetMapping("{id}")
    public ResponseEntity<Document> getDocument(@PathVariable("id") String id) {
        Document doc = genericResourceService.get("document", id);
        return new ResponseEntity<>(doc, HttpStatus.OK);
    }


}
