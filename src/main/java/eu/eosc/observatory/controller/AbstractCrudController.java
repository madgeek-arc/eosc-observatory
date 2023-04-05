package eu.eosc.observatory.controller;

import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.Identifiable;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.controller.GenericItemController;

import gr.athenarc.catalogue.utils.PagingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.Map;

public abstract class AbstractCrudController<T extends Identifiable<?>> {

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

    @Browse
    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<T>> browse(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = PagingUtils.createFacetFilter(allRequestParams);
        Browsing<T> tBrowsing = crudItemService.getAll(filter);
        return new ResponseEntity<>(tBrowsing, HttpStatus.OK);
    }
}
