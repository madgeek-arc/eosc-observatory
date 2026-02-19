/*
 * Copyright 2021-2026 OpenAIRE AMKE
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

import eu.openaire.observatory.datasets.ingest.custom.CustomIngestService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(path = "datasets", produces = MediaType.APPLICATION_JSON_VALUE)
public class DatasetController {

    private final DefaultDatasetService datasetService;
    private final CustomIngestService ingestService;

    public DatasetController(DefaultDatasetService datasetService,
                             CustomIngestService ingestService) {
        this.datasetService = datasetService;
        this.ingestService = ingestService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void importDataset(@RequestPart("file") MultipartFile file) throws IOException {
        ingestService.ingest(file);
    }

    @GetMapping("{id}")
    public ResponseEntity<DatasetEntry> getEntry(@PathVariable long id) {
        return ResponseEntity.ok(datasetService.get(id));
    }

    @GetMapping
    public ResponseEntity<List<DatasetEntry>> getAll() {
        return ResponseEntity.ok(datasetService.getAll());
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable long id) {
        datasetService.delete(id);
    }
}
