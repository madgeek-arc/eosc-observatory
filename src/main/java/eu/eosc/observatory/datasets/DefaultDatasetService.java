package eu.eosc.observatory.datasets;

import gr.athenarc.catalogue.exception.ResourceAlreadyExistsException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultDatasetService implements DatasetService {

    private final DatasetRepository datasetRepository;


    public DefaultDatasetService(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    @Override
    public DatasetEntry add(DatasetEntry datasetEntry) {
        if (datasetEntry.getId() != null && datasetRepository.findById(datasetEntry.getId()).isPresent()) {
            throw new ResourceAlreadyExistsException();
        }
        return datasetRepository.save(datasetEntry);
    }

    @Override
    public DatasetEntry get(long id) {
        return datasetRepository.findById(id).get();
    }

    @Override
    public List<DatasetEntry> getAll() {
        return datasetRepository.findAll();
    }

    @Override
    public void delete(Long id) {
        datasetRepository.deleteById(id);
    }
}
