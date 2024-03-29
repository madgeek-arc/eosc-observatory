package eu.eosc.observatory.datasets;

import java.util.List;

public interface DatasetService {

    DatasetEntry save(DatasetEntry datasetEntry);

    DatasetEntry get(long id);

    List<DatasetEntry> getAll();

    void delete(Long id);

}
