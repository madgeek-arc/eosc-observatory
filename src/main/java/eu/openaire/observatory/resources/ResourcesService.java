/*
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
package eu.openaire.observatory.resources;

import com.fasterxml.jackson.databind.JsonNode;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.resources.model.Document;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;

@Service
public class ResourcesService {

    public final GenericResourceService genericResourceService;

    public ResourcesService(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
    }

    public List<Document> getRecommendations(FacetFilter filter, String id) {
        return genericResourceService.recommend(filter, id);
    }

    public Document update(String id, JsonNode docInfo) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        if (docInfo == null) {
            throw new ResourceException("Cannot assign 'null' body", HttpStatus.CONFLICT);
        }
        Document doc = genericResourceService.get("document", id);
        doc.setDocInfo(docInfo);
        doc.setCurated(true);
        doc.getMetadata().setModificationDate(new Date());
        doc.getMetadata().setModifiedBy(User.getId(SecurityContextHolder.getContext().getAuthentication()));
        return genericResourceService.update("document", id, doc);
    }

    public Document setStatus(String id, Document.Status status) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        if (status == Document.Status.PENDING) {
            throw new ResourceException("Cannot assign 'PENDING' status", HttpStatus.CONFLICT);
        }
        Document doc = genericResourceService.get("document", id);
        doc.setStatus(status.name());
        return genericResourceService.update("document", id, doc);
    }
}
