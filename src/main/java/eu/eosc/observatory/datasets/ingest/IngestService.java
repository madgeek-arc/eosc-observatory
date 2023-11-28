package eu.eosc.observatory.datasets.ingest;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IngestService {

    void ingest(MultipartFile file) throws IOException;
}
