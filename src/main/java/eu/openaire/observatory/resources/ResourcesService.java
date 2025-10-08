package eu.openaire.observatory.resources;

import eu.openaire.observatory.resources.model.Document;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

@Service
public class ResourcesService {

    public GenericResourceService genericResourceService;

    public ResourcesService(GenericResourceService genericResourceService) {
        this.genericResourceService = genericResourceService;
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
