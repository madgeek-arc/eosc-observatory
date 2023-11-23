package eu.eosc.observatory.datasets;

import eu.eosc.observatory.datasets.ingest.custom.CustomIngestService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("datasets")
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
