package eu.openaire.observatory.resources;

import eu.openaire.observatory.resources.model.Document;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.*;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RestController
@RequestMapping(path = "documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class ResourcesController {

    private final GenericResourceService genericResourceService;
    private final ResourcesService resourcesService;
    private final DocumentsCsvConverter documentsCsvConverter;

    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceTypeService resourceTypeService;

    public ResourcesController(GenericResourceService genericResourceService,
                               ResourcesService resourcesService,
                               DocumentsCsvConverter documentsCsvConverter) {
        this.genericResourceService = genericResourceService;
        this.resourcesService = resourcesService;
        this.documentsCsvConverter = documentsCsvConverter;
    }

    public record StatusChange(@NotNull Document.Status status) {}

    @GetMapping
    @BrowseParameters
//    @PreAuthorize("canReadDocuments(#allRequestParams.get('status'))")
    public ResponseEntity<Paging<HighlightedResult<Document>>> getDocuments(@Parameter(hidden = true)
                                                         @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType("document");
        filter.addFilter("status", Document.Status.APPROVED);
        Paging<HighlightedResult<Document>> docs = genericResourceService.getHighlightedResults(filter);
        return new ResponseEntity<>(docs, HttpStatus.OK);
    }

    @PutMapping("{id}/status")
    @PreAuthorize("isAdministratorOfType('eosc-sb')")
    public ResponseEntity<Document> approve(@PathVariable String id,
                                            @RequestBody @Valid StatusChange body)
            throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        return new ResponseEntity<>(resourcesService.setStatus(id, body.status()), HttpStatus.OK);
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
