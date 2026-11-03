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

import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvControllerTest {

    @Test
    void resolveDataYearIsTheYearBeforeTheModelYear() {
        assertThat(CsvController.resolveDataYear("m-eosc-sb-2025", null)).isEqualTo("2024");
        assertThat(CsvController.resolveDataYear("m-eosc-sb-2025", " ")).isEqualTo("2024");
    }

    @Test
    void resolveDataYearPrefersTheGivenYear() {
        assertThat(CsvController.resolveDataYear("m-eosc-sb-2025", "2023")).isEqualTo("2023");
        assertThat(CsvController.resolveDataYear("m-jlFggsCN", "2021")).isEqualTo("2021");
    }

    @Test
    void resolveDataYearRejectsModelWithoutYear() {
        assertThatThrownBy(() -> CsvController.resolveDataYear("m-jlFggsCN", null))
                .isInstanceOf(ResourceException.class);
    }

    @Test
    void resolveDataYearRejectsInvalidYear() {
        assertThatThrownBy(() -> CsvController.resolveDataYear("m-eosc-sb-2025", "24"))
                .isInstanceOf(ResourceException.class);
    }
}
