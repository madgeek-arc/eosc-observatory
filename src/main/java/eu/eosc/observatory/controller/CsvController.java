package eu.eosc.observatory.controller;

import eu.eosc.observatory.service.CSVConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("csv")
public class CsvController {


    private final CSVConverter csvConverter;

    @Autowired
    public CsvController(CSVConverter csvConverter) {
        this.csvConverter = csvConverter;
    }

    @GetMapping(
            value = "/export/answers/{id}",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> exportSurveysToCsv(@PathVariable("id") String modelId, @RequestParam(value = "includeUsers", defaultValue = "false") boolean includeUsers, HttpServletResponse response) {
        StringBuilder filename = new StringBuilder();
        filename.append(modelId);
        filename.append(includeUsers ? "_users" : "");
        filename.append(".tsv");
        response.setHeader("Content-disposition", "attachment; filename=" + filename);
        return ResponseEntity.ok(csvConverter.convertToCSV(modelId, includeUsers).getBytes());
    }

    @PostMapping(value = "/import/answers/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<?>> importSurveysFromCsv(@PathVariable("id") String modelId, @RequestBody String data) {
        return ResponseEntity.ok(csvConverter.ingestFromCSV(modelId, data));
    }
}
