package eu.eosc.observatory.datasets;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetRepository extends CrudRepository<DatasetEntry, Long> {

    List<DatasetEntry> findAll();

    Optional<DatasetEntry> findByYearAndCountryAndNameAndAuthority(String year, String country, String name, String authority);
}
