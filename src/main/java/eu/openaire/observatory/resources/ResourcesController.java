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
import jakarta.servlet.http.HttpServletResponse;
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
    private final DocumentsCsvConverter documentsCsvConverter;

    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceTypeService resourceTypeService;

    public ResourcesController(GenericResourceService genericResourceService,
                               DocumentsCsvConverter documentsCsvConverter) {
        this.genericResourceService = genericResourceService;
        this.documentsCsvConverter = documentsCsvConverter;
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

    @GetMapping("convert/csv")
    public ResponseEntity<String> getCSV(HttpServletResponse response) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
//        filter.addFilter("status", Document.Status.APPROVED);
        filter.setResourceType("document");
        Paging<Document> docs = genericResourceService.getResults(filter);

        String filename = "documents.tsv";
        response.setHeader("Content-disposition", "attachment; filename=" + filename);

        return new ResponseEntity<>(documentsCsvConverter.toTsv(docs.getResults()), HttpStatus.OK);
    }

    @GetMapping("{id}")
    public ResponseEntity<Document> getDocument(@PathVariable("id") String id) {
        Document doc = genericResourceService.get("document", id);
        return new ResponseEntity<>(doc, HttpStatus.OK);
    }

    @PostMapping("migrate")
    public ResponseEntity<Void> migrate(String resourceTypeFrom, String resourceTypeTo) {
        ResourceType rtFrom = resourceTypeService.getResourceType(resourceTypeFrom);
        ResourceType rtTo = resourceTypeService.getResourceType(resourceTypeTo);

        List<Resource> resources = resourceService.getResource(rtFrom);
        for (Resource res : resources) {
            resourceService.changeResourceType(res, rtTo);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }


}
