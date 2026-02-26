/*
 * Copyright 2021-2026 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.openaire.observatory.resources;

import com.fasterxml.jackson.databind.JsonNode;
import eu.openaire.observatory.resources.dto.DocumentResponse;
import eu.openaire.observatory.resources.model.Document;
import eu.openaire.observatory.resources.dto.DocumentSummaryResponse;
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
import org.springframework.security.access.prepost.PostAuthorize;
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

    public record StatusChange(@NotNull Document.Status status) {
    }

    public enum View {
        FULL, DETAIL, SUMMARY
    }

    @GetMapping
    @BrowseParameters
    @PreAuthorize("canReadDocuments(#allRequestParams.get('status'))")
    public ResponseEntity<Paging<HighlightedResult<?>>> getDocuments(
            @RequestParam(required = false, defaultValue = "APPROVED") Document.Status status,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams,
            @RequestParam(required = false, defaultValue = "SUMMARY") View view) {
        allRequestParams.remove("view");
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType("document");
        Paging<HighlightedResult<Document>> docs = genericResourceService.getHighlightedResults(filter);
        Paging<HighlightedResult<?>> response = switch (view) {
            case FULL -> docs.map(i -> i); // no-op, just a trick for casting
            case DETAIL -> docs.map(i -> i.map(DocumentResponse::new));
            default -> docs.map(i -> i.map(DocumentSummaryResponse::new));
        };
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("{id}/recommendations")
    @BrowseParameters
    @PreAuthorize("canReadDocuments(#allRequestParams.get('status'))")
    public ResponseEntity<List<Document>> recommendations(
            @PathVariable String id,
            @Parameter(hidden = false) @RequestParam(required = false, defaultValue = "APPROVED") Document.Status status,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType("document");
        return new ResponseEntity<>(resourcesService.getRecommendations(filter, id), HttpStatus.OK);
    }

    @PutMapping("{id}/docInfo")
    @PreAuthorize("isAdministratorOfType('eosc-sb')")
    public ResponseEntity<Document> update(@PathVariable String id,
                                           @RequestBody JsonNode body)
            throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        return new ResponseEntity<>(resourcesService.update(id, body), HttpStatus.OK);
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
    @PostAuthorize("isAdministratorOfType('eosc-sb') or canReadDocuments(returnObject.getStatus())")
    public ResponseEntity<Document> getDocument(@PathVariable("id") String id) {
        Document doc = genericResourceService.get("document", id);
        return new ResponseEntity<>(doc, HttpStatus.OK);
    }

    @PostMapping("migrate")
    @PreAuthorize("hasAuthority('ADMIN')")
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
