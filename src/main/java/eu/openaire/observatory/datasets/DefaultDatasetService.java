/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.datasets;

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
