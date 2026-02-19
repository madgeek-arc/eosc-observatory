package eu.openaire.observatory.resources.model;

import eu.openaire.observatory.domain.Metadata;
import org.springframework.security.core.Authentication;

public class DocumentMetadata extends Metadata {

    private String model;

    public DocumentMetadata() {
        super();
    }

    public DocumentMetadata(Authentication authentication) {
        super(authentication);
    }

    public DocumentMetadata(Authentication authentication, String model) {
        super(authentication);
        this.model = model;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
