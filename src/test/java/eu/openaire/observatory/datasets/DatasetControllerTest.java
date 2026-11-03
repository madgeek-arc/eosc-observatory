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

package eu.openaire.observatory.datasets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetControllerTest {

    @Test
    void toFilterKeepsTheGivenParameters() {
        DatasetEntry filter = DatasetController.toFilter("2024", "GR", "researchers", "EUROSTAT", "50892.2", "note");

        assertThat(filter.getId()).isNull();
        assertThat(filter.getYear()).isEqualTo("2024");
        assertThat(filter.getCountry()).isEqualTo("GR");
        assertThat(filter.getName()).isEqualTo("researchers");
        assertThat(filter.getAuthority()).isEqualTo("EUROSTAT");
        assertThat(filter.getValue()).isEqualTo("50892.2");
        assertThat(filter.getComment()).isEqualTo("note");
    }

    @Test
    void toFilterLeavesOutMissingAndBlankParameters() {
        DatasetEntry filter = DatasetController.toFilter(null, "", "  ", null, "", "");

        assertThat(filter.getYear()).isNull();
        assertThat(filter.getCountry()).isNull();
        assertThat(filter.getName()).isNull();
        assertThat(filter.getAuthority()).isNull();
        assertThat(filter.getValue()).isNull();
        assertThat(filter.getComment()).isNull();
    }

    @Test
    void toFilterTrimsParameters() {
        DatasetEntry filter = DatasetController.toFilter(" 2024 ", " gr", null, null, null, null);

        assertThat(filter.getYear()).isEqualTo("2024");
        assertThat(filter.getCountry()).isEqualTo("gr");
    }
}
