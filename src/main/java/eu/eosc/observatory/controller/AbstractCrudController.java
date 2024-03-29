package eu.eosc.observatory.controller;

import eu.eosc.observatory.service.CrudService;
import eu.eosc.observatory.service.Identifiable;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.annotations.Browse;

import gr.athenarc.catalogue.utils.PagingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.Map;

public abstract class AbstractCrudController<T extends Identifiable<?>> {

    private final CrudService<T> crudService;

    protected AbstractCrudController(CrudService<T> crudService) {
        this.crudService = crudService;
    }

    @GetMapping("{id}")
    public ResponseEntity<T> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(crudService.get(id), HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<T> create(@RequestBody T t) {
        return new ResponseEntity<>(crudService.add(t), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<T> delete(@PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(crudService.delete(id), HttpStatus.OK);
    }

    @Browse
    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<T>> browse(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = PagingUtils.createFacetFilter(allRequestParams);
        Browsing<T> tBrowsing = crudService.getAll(filter);
        return new ResponseEntity<>(tBrowsing, HttpStatus.OK);
    }
}
