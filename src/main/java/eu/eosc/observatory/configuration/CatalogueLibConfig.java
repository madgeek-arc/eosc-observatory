package eu.eosc.observatory.configuration;

import gr.athenarc.catalogue.config.CatalogueLibConfiguration;
import org.springframework.stereotype.Component;

@Component
public class CatalogueLibConfig implements CatalogueLibConfiguration {

    @Override
    public String generatedClassesPackageName() {
        return "eu.eosc.observatory.xsd2java";
    }
}
