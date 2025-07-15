package eu.openaire.observatory.analyzer;

import eu.openaire.observatory.analyzer.model.UrlReferences;

import java.util.List;

public interface UrlExtractor <T> {

    List<UrlReferences> extract(T object);

}
