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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetExternalValueProviderTest {

    private final DatasetService datasetService = mock(DatasetService.class);
    private final DatasetExternalValueProvider provider = new DatasetExternalValueProvider(datasetService);

    @ParameterizedTest
    @CsvSource({
            "Question1-0, researchers, EUROSTAT",
            "Question57-0, publications, OpenAIRE",
            "Question69-0, datasets, OpenAIRE",
            "Question73-0, software, OpenAIRE",
            "Question77-0, services, EOSC",
            "Question97-0, signatories, COARA"
    })
    void getValuesFiltersByTheDatasetOfTheField(String fieldName, String name, String authority) {
        when(datasetService.getAll(any(DatasetEntry.class))).thenReturn(List.of());

        provider.getValues(fieldName, "2024");

        ArgumentCaptor<DatasetEntry> captor = ArgumentCaptor.forClass(DatasetEntry.class);
        verify(datasetService).getAll(captor.capture());
        DatasetEntry filter = captor.getValue();
        assertThat(filter.getYear()).isEqualTo("2024");
        assertThat(filter.getName()).isEqualTo(name);
        assertThat(filter.getAuthority()).isEqualTo(authority);
        assertThat(filter.getCountry()).isNull();
        assertThat(filter.getValue()).isNull();
        assertThat(filter.getComment()).isNull();
    }

    @Test
    void getValuesIsEmptyForFieldsWithoutDataset() {
        assertThat(provider.getValues("Question104-0", "2024")).isEmpty();

        verify(datasetService, never()).getAll(any(DatasetEntry.class));
    }

    @Test
    void getValuesKeysTheValuesByUpperCaseCountry() {
        when(datasetService.getAll(any(DatasetEntry.class))).thenReturn(List.of(
                entry("GR", "50892.2"),
                entry("it", "179268"),
                entry("CH", "-")));

        Map<String, String> values = provider.getValues("Question1-0", "2024");

        assertThat(values).containsOnly(
                Map.entry("GR", "50892.2"),
                Map.entry("IT", "179268"),
                Map.entry("CH", "-"));
    }

    @Test
    void getValuesKeepsTheFirstOfDuplicateCountries() {
        when(datasetService.getAll(any(DatasetEntry.class))).thenReturn(List.of(
                entry("GR", "1"),
                entry("gr", "2")));

        assertThat(provider.getValues("Question97-0", "2024")).containsOnly(Map.entry("GR", "1"));
    }

    @Test
    void getValuesSkipsEntriesWithoutCountry() {
        when(datasetService.getAll(any(DatasetEntry.class))).thenReturn(List.of(
                entry(null, "1"),
                entry("GR", "2")));

        assertThat(provider.getValues("Question97-0", "2024")).containsOnly(Map.entry("GR", "2"));
    }

    private static DatasetEntry entry(String country, String value) {
        return new DatasetEntry("2024", country, "researchers", "EUROSTAT", value, "");
    }
}
