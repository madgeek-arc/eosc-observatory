package eu.openaire.observatory.resources.analyzer;

import eu.openaire.observatory.resources.analyzer.model.UrlReferences;

import java.util.List;

public interface UrlExtractor <T> {

    List<UrlReferences> extract(T object);

}
