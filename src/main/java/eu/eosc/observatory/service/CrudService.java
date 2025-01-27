/**
 * Copyright 2021-2025 OpenAIRE AMKE
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
package eu.eosc.observatory.service;

import eu.eosc.observatory.dto.HistoryDTO;
import eu.eosc.observatory.dto.HistoryEntryDTO;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;

import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface CrudService<T extends Identifiable> {
    /**
     * Returns the resource.
     *
     * @param id of the resource in the index.
     * @return the resource.
     */
    T get(String id);

    T getVersion(String id, String version);

    /**
     * Returns the resource.
     *
     * @param id of the resource in the index.
     * @return the resource.
     */
    Resource getResource(String id);

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

    /**
     * Retrieve a Set of resources matching the specified Key Value arguments.
     *
     * @param key
     * @param value
     * @return
     */
    Set<T> getWithFilter(String key, String value);

    <T> HistoryDTO getHistory(String resourceId, Function<T, HistoryEntryDTO> transform);

    T restore(String resourceId, String versionId, UnaryOperator<T> transform);
}
