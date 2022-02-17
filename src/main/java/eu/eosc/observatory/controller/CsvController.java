package eu.eosc.observatory.controller;

import eu.eosc.observatory.service.CSVConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("csv")
public class CsvController {


    private final CSVConverter csvConverter;

    @Autowired
    public CsvController(CSVConverter csvConverter) {
        this.csvConverter = csvConverter;
    }

    @GetMapping("export/answers/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> exportSurveysToCsv(@PathVariable("id") String modelId, @RequestParam(value = "includeUsers", defaultValue = "false") boolean includeUsers) {
        return new ResponseEntity<>(csvConverter.convertToCSV(modelId, includeUsers), HttpStatus.OK);
    }
}