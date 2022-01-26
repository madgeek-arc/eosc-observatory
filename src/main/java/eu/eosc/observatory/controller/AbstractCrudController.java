package eu.eosc.observatory.controller;

import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.Identifiable;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.controller.GenericItemController;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

public abstract class AbstractCrudController <T extends Identifiable<?>> {

    private final CrudItemService<T> crudItemService;

    protected AbstractCrudController(CrudItemService<T> crudItemService) {
        this.crudItemService = crudItemService;
    }

    @GetMapping("{id}")
    public ResponseEntity<T> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(crudItemService.get(id), HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<T> create(@RequestBody T t) {
        return new ResponseEntity<>(crudItemService.add(t), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<T> delete(@PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(crudItemService.delete(id), HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<T>> browse(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Browsing<T> tBrowsing = crudItemService.getAll(filter);
        return new ResponseEntity<>(tBrowsing, HttpStatus.OK);
    }
}
