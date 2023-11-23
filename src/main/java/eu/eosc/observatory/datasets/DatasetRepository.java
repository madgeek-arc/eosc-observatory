package eu.eosc.observatory.datasets;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRepository extends CrudRepository<DatasetEntry, Long> {

    List<DatasetEntry> findAll();
}
