package eu.openaire.observatory.resources;

import com.fasterxml.jackson.databind.JsonNode;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.resources.model.Document;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

@Service
public class ResourcesService {

    public GenericResourceService genericResourceService;

    public ResourcesService(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
    }

    public Document update(String id, JsonNode docInfo) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        if (docInfo == null) {
            throw new ResourceException("Cannot assign 'null' body", HttpStatus.CONFLICT);
        }
        Document doc = genericResourceService.get("document", id);
        doc.setDocInfo(docInfo);
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
