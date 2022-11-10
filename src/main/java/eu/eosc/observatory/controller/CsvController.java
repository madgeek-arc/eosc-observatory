package eu.eosc.observatory.controller;

import eu.eosc.observatory.service.CSVConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("csv")
public class CsvController {

    private static final Logger logger = LoggerFactory.getLogger(CsvController.class);

    private final CSVConverter csvConverter;

    @Autowired
    public CsvController(CSVConverter csvConverter) {
        this.csvConverter = csvConverter;
    }

    @GetMapping(
            value = "/export/answers/{id}",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @PreAuthorize("hasAuthority('ADMIN') or hasCoordinatorAccess(#modelId) or hasStakeholdeManagerAccess(#modelId)")
    public ResponseEntity<byte[]> exportSurveysToCsv(@PathVariable("id") String modelId,
                                                     @RequestParam(value = "dateFrom", required = false) String dateFrom,
                                                     @RequestParam(value = "dateTo", required = false) String dateTo,
                                                     HttpServletResponse response) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date from = dateFrom != null ? formatter.parse(dateFrom) : null;
        Date to = dateTo != null ? formatter.parse(dateTo) : null;

        StringBuilder filename = new StringBuilder();
        filename.append(modelId);
        filename.append(".tsv");
        response.setHeader("Content-disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok(csvConverter.convertToCSV(modelId, false, from, to).getBytes());
    }

    @GetMapping(
            value = "/export/extended/answers/{id}",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> exportExtendedSurveysToCsv(@PathVariable("id") String modelId,
                                                             @RequestParam(value = "includeUsers", defaultValue = "false") boolean includeUsers,
                                                             @RequestParam(value = "dateFrom", required = false) String dateFrom,
                                                             @RequestParam(value = "dateTo", required = false) String dateTo,
                                                             HttpServletResponse response) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date from = dateFrom != null ? formatter.parse(dateFrom) : null;
        Date to = dateTo != null ? formatter.parse(dateTo) : null;

        StringBuilder filename = new StringBuilder();
        filename.append(modelId);
        filename.append(includeUsers ? "_users" : "");
        filename.append(".tsv");
        response.setHeader("Content-disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok(csvConverter.convertToCSV(modelId, includeUsers, from, to).getBytes());
    }

    @PostMapping(value = "/import/answers/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<?>> importSurveysFromCsv(@PathVariable("id") String modelId, @RequestBody String data) {
        return ResponseEntity.ok(csvConverter.ingestFromCSV(modelId, data));
    }
}
