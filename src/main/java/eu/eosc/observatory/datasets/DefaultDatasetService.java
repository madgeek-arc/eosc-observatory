package eu.eosc.observatory.datasets;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DefaultDatasetService implements DatasetService {

    private final DatasetRepository datasetRepository;


    public DefaultDatasetService(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    @Override
    public DatasetEntry save(DatasetEntry datasetEntry) {
        Optional<DatasetEntry> existing = datasetRepository.findByYearAndCountryAndNameAndAuthority(datasetEntry.getYear(), datasetEntry.getCountry(), datasetEntry.getName(), datasetEntry.getAuthority());
        existing.ifPresent(entry -> datasetEntry.setId(entry.getId()));
        return datasetRepository.save(datasetEntry);
    }

    @Override
    public DatasetEntry get(long id) {
        return datasetRepository.findById(id).orElse(null);
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
