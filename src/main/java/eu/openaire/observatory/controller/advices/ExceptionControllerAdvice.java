/*
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
package eu.openaire.observatory.controller.advices;

import gr.uoa.di.madgik.catalogue.controller.GenericExceptionController;
import gr.uoa.di.madgik.catalogue.exception.ServerError;
import io.sentry.Sentry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ExceptionControllerAdvice extends GenericExceptionController {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ServerError> handleException(HttpServletRequest req, Exception ex) {
        // Forwards exception to Sentry
        Sentry.captureException(ex);
        return super.handleException(req, ex);
    }
}
