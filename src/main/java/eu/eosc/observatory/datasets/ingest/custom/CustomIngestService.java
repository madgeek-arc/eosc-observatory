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
                datasetService.add(new DatasetEntry(entry.getYear(), entry.getCountry(), "researchers", "EUROSTAT", entry.getResearchers(), ""));
            if (StringUtils.hasText(entry.getPublications()))
                datasetService.add(new DatasetEntry(entry.getYear(), entry.getCountry(), "publications", "OpenAIRE", entry.getPublications(), ""));
            if (StringUtils.hasText(entry.getDatasets()))
                datasetService.add(new DatasetEntry(entry.getYear(), entry.getCountry(), "datasets", "OpenAIRE", entry.getDatasets(), ""));
            if (StringUtils.hasText(entry.getSoftware()))
                datasetService.add(new DatasetEntry(entry.getYear(), entry.getCountry(), "software", "OpenAIRE", entry.getSoftware(), ""));
            if (StringUtils.hasText(entry.getServices()))
                datasetService.add(new DatasetEntry(entry.getYear(), entry.getCountry(), "services", "EOSC", entry.getServices(), ""));
            if (StringUtils.hasText(entry.getSignatories()))
                datasetService.add(new DatasetEntry(entry.getYear(), entry.getCountry(), "signatories", "COARA", entry.getSignatories(), ""));
        }
    }
}
