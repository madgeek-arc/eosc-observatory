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
package eu.eosc.observatory.datasets.ingest.custom;

import com.opencsv.bean.CsvToBeanBuilder;
import eu.eosc.observatory.datasets.DatasetEntry;
import eu.eosc.observatory.datasets.DatasetService;
import eu.eosc.observatory.datasets.ingest.IngestService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Service
public class CustomIngestService implements IngestService {

    private final DatasetService datasetService;

    public CustomIngestService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @Override
    public void ingest(MultipartFile file) throws IOException {
        List<CustomCsv> entries = new CsvToBeanBuilder<CustomCsv>(new InputStreamReader(file.getInputStream()))
                .withSeparator('\t')
                .withType(CustomCsv.class)
                .build()
                .parse();
        for (CustomCsv entry : entries) {
            if (StringUtils.hasText(entry.getResearchers()))
                datasetService.save(new DatasetEntry(entry.getYear(), entry.getCountry(), "researchers", "EUROSTAT", entry.getResearchers(), ""));
            if (StringUtils.hasText(entry.getPublications()))
                datasetService.save(new DatasetEntry(entry.getYear(), entry.getCountry(), "publications", "OpenAIRE", entry.getPublications(), ""));
            if (StringUtils.hasText(entry.getDatasets()))
                datasetService.save(new DatasetEntry(entry.getYear(), entry.getCountry(), "datasets", "OpenAIRE", entry.getDatasets(), ""));
            if (StringUtils.hasText(entry.getSoftware()))
                datasetService.save(new DatasetEntry(entry.getYear(), entry.getCountry(), "software", "OpenAIRE", entry.getSoftware(), ""));
            if (StringUtils.hasText(entry.getServices()))
                datasetService.save(new DatasetEntry(entry.getYear(), entry.getCountry(), "services", "EOSC", entry.getServices(), ""));
            if (StringUtils.hasText(entry.getSignatories()))
                datasetService.save(new DatasetEntry(entry.getYear(), entry.getCountry(), "signatories", "COARA", entry.getSignatories(), ""));
        }
    }
}
