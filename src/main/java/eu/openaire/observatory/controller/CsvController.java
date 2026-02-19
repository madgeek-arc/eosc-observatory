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

package eu.openaire.observatory.controller;

import eu.openaire.observatory.service.CSVConverter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(path = "csv", produces = MediaType.APPLICATION_JSON_VALUE)
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
    @PreAuthorize("hasAuthority('ADMIN') or hasCoordinatorAccessOnSurvey(#modelId) or hasStakeholderManagerAccessOnSurvey(#modelId)")
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
