package eu.eosc.observatory.service;

import java.util.Date;
import java.util.List;

public interface CSVConverter {

    String convertToCSV(String modelId, boolean includeSensitiveData, Date from, Date to);

    List<?> ingestFromCSV(String modelId, String data);

}
