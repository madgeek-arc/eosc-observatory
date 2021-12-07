package eu.eosc.observatory.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;

public interface CrudItemService<T extends Identifiable> {
    /**
     * Returns the resource.
     *
     * @param id of the resource in the index.
     * @return the resource.
     */
    T get(String id);

    /**
     * Returns all resource.
     *
     * @param filter parameters for the indexer.
     * @return the results paged.
     */
    Browsing<T> getAll(FacetFilter filter);

    /**
     * Returns all resource of a user.
     *
     * @param filter parameters for the indexer.
     * @return the results paged.
     */
    Browsing<T> getMy(FacetFilter filter);

    /**
     * Add a new resource.
     *
     * @param resource to be added
     * @return the id of the inserted resource.
     */
    T add(T resource);

    /**
     * Update the resource.
     *
     * @param id       of resource to be updated.
     * @param resource to be updated.
     * @return the updated resource.
     */
    T update(String id, T resource) throws ResourceNotFoundException;

    /**
     * Delete the resource.
     *
     * @param id to be deleted.
     */
    T delete(String id) throws ResourceNotFoundException;
}
