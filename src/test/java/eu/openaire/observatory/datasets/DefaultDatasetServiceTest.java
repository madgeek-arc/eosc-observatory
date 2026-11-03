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
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultDatasetServiceTest {

    private final DatasetRepository repository = mock(DatasetRepository.class);
    private final DefaultDatasetService service = new DefaultDatasetService(repository);

    @Test
    @SuppressWarnings("unchecked")
    void getAllWithFilterQueriesByExampleIgnoringCaseAndNulls() {
        DatasetEntry match = new DatasetEntry("2024", "GR", "researchers", "EUROSTAT", "50892.2", "");
        when(repository.findAll(any(Example.class))).thenReturn(List.of(match));
        DatasetEntry filter = new DatasetEntry("2024", "gr", null, null, null, null);

        List<DatasetEntry> entries = service.getAll(filter);

        assertThat(entries).containsExactly(match);
        ArgumentCaptor<Example<DatasetEntry>> captor = ArgumentCaptor.forClass(Example.class);
        verify(repository).findAll(captor.capture());
        Example<DatasetEntry> example = captor.getValue();
        assertThat(example.getProbe()).isSameAs(filter);
        ExampleMatcher matcher = example.getMatcher();
        assertThat(matcher.isAllMatching()).isTrue();
        assertThat(matcher.getNullHandler()).isEqualTo(ExampleMatcher.NullHandler.IGNORE);
        assertThat(matcher.getDefaultStringMatcher()).isEqualTo(ExampleMatcher.StringMatcher.DEFAULT);
        assertThat(matcher.isIgnoreCaseEnabled()).isTrue();
    }
}
