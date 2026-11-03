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
import gr.uoa.di.madgik.registry.exception.ResourceException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping(path = "csv", produces = MediaType.APPLICATION_JSON_VALUE)
public class CsvController {

    private static final Logger logger = LoggerFactory.getLogger(CsvController.class);
    private static final Pattern YEAR = Pattern.compile("\\d{4}");
    private static final Pattern MODEL_YEAR = Pattern.compile("-(\\d{4})$");

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
                                                     @RequestParam(value = "validatedOnly", defaultValue = "false") boolean validatedOnly,
                                                     @RequestParam(value = "dateFrom", required = false) String dateFrom,
                                                     @RequestParam(value = "dateTo", required = false) String dateTo,
                                                     HttpServletResponse response) throws ParseException {
        Date[] dates = parseDateRange(dateFrom, dateTo);

        StringBuilder filename = new StringBuilder();
        filename.append(modelId);
        filename.append(validatedOnly ? "_validated" : "");
        filename.append(".tsv");
        response.setHeader("Content-disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok(csvConverter.convertToCSV(modelId, false, validatedOnly, dates[0], dates[1]).getBytes());
    }

    @GetMapping(
            value = "/export/extended/answers/{id}",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> exportExtendedSurveysToCsv(@PathVariable("id") String modelId,
                                                             @RequestParam(value = "includeUsers", defaultValue = "false") boolean includeUsers,
                                                             @RequestParam(value = "validatedOnly", defaultValue = "false") boolean validatedOnly,
                                                             @RequestParam(value = "dateFrom", required = false) String dateFrom,
                                                             @RequestParam(value = "dateTo", required = false) String dateTo,
                                                             HttpServletResponse response) throws ParseException {
        Date[] dates = parseDateRange(dateFrom, dateTo);

        StringBuilder filename = new StringBuilder();
        filename.append(modelId);
        filename.append(includeUsers ? "_users" : "");
        filename.append(validatedOnly ? "_validated" : "");
        filename.append(".tsv");
        response.setHeader("Content-disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok(csvConverter.convertToCSV(modelId, includeUsers, validatedOnly, dates[0], dates[1]).getBytes());
    }

    @GetMapping(
            value = "/export/external/{id}",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> exportExternalQuestionsToCsv(@PathVariable("id") String modelId,
                                                               @RequestParam(value = "year", required = false) String year,
                                                               HttpServletResponse response) {
        String dataYear = resolveDataYear(modelId, year);

        String filename = String.format("%s_external_%s.tsv", modelId, dataYear);
        response.setHeader("Content-disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok(csvConverter.convertExternalToCSV(modelId, dataYear).getBytes());
    }

    /**
     * Returns the given year, or else the year before the one at the end of the model id
     * (e.g. 2024 for "m-eosc-sb-2025"), since the questions of a survey ask about the previous year.
     */
    static String resolveDataYear(String modelId, String year) {
        if (year != null && !year.isBlank()) {
            if (!YEAR.matcher(year).matches()) {
                throw new ResourceException("year must have four digits.", HttpStatus.BAD_REQUEST);
            }
            return year;
        }
        Matcher matcher = MODEL_YEAR.matcher(modelId);
        if (!matcher.find()) {
            throw new ResourceException("The model id has no year, please provide the year.", HttpStatus.BAD_REQUEST);
        }
        return String.valueOf(Integer.parseInt(matcher.group(1)) - 1);
    }

    private Date[] parseDateRange(String dateFrom, String dateTo) throws ParseException {
        // a range is only valid when both dates are given, otherwise it is ignored
        if (dateFrom == null || dateFrom.isBlank() || dateTo == null || dateTo.isBlank()) {
            return new Date[]{null, null};
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date from = formatter.parse(dateFrom);
        Date to = formatter.parse(dateTo);
        if (from.after(to)) {
            throw new ResourceException("dateFrom must not be after dateTo.", HttpStatus.BAD_REQUEST);
        }
        // dateTo covers its whole day: move it to the last millisecond of that day
        Calendar endOfDay = new GregorianCalendar();
        endOfDay.setTime(to);
        endOfDay.add(Calendar.DAY_OF_MONTH, 1);
        endOfDay.add(Calendar.MILLISECOND, -1);
        return new Date[]{from, endOfDay.getTime()};
    }

    @PostMapping(value = "/import/answers/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<?>> importSurveysFromCsv(@PathVariable("id") String modelId, @RequestBody String data) {
        return ResponseEntity.ok(csvConverter.ingestFromCSV(modelId, data));
    }
}
