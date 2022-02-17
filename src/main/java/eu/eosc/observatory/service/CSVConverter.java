package eu.eosc.observatory.service;

public interface CSVConverter {

    String convertToCSV(String modelId, boolean includeSensitiveData);

}
