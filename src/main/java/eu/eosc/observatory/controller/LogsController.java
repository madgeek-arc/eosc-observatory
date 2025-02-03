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
package eu.eosc.observatory.controller;

import org.apache.logging.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;

public class LogsController extends gr.uoa.di.madgik.catalogue.controller.LogsController {

    private static final Logger logger = LoggerFactory.getLogger(LogsController.class);

    @Override
//    @PostMapping("level/root")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> setRootLogLevel(@RequestParam Level standardLevel) {
        return super.setRootLogLevel(standardLevel);
    }

    @Override
//    @PostMapping("level/package/{path}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> setPackageLogLevel(@PathVariable("path") String path, @RequestParam Level standardLevel) {
        return super.setPackageLogLevel(path, standardLevel);
    }

    @Override
//    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> fetchLogFile(@RequestParam(required = false) String filepath, HttpServletResponse response) {
        return super.fetchLogFile(filepath, response);
    }

}
