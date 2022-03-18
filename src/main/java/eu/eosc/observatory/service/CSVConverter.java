package eu.eosc.observatory.service;

import java.util.List;

public interface CSVConverter {

    String convertToCSV(String modelId, boolean includeSensitiveData);

    List<?> ingestFromCSV(String modelId, String data);

}
