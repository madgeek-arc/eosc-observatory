package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.service.SurveyService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyIdDimensionMemberProviderTest {

    @Mock
    private SurveyService surveyService;

    private SurveyIdDimensionMemberProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SurveyIdDimensionMemberProvider(surveyService);
    }

    @Test
    void returnsSurveysAsMembersWhenNoSearchText() {
        when(surveyService.getByType(any(FacetFilter.class), eq("eosc-sb"))).thenReturn(browsingOf(
            model("m-eosc-sb-2023", "Survey on National Contributions to EOSC and Open Science 2023"),
            model("m-eosc-sb-2024", "Survey on National Contributions to EOSC and Open Science 2024")
        ));

        DimensionMembers page = provider.search(null, "", 100);

        assertThat(page.members()).containsExactly(
            new DimensionMember("m-eosc-sb-2023", "Survey on National Contributions to EOSC and Open Science 2023"),
            new DimensionMember("m-eosc-sb-2024", "Survey on National Contributions to EOSC and Open Science 2024")
        );
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        when(surveyService.getByType(any(FacetFilter.class), eq("eosc-sb"))).thenReturn(browsingOf(
            model("m-eosc-sb-2023", "Survey on National Contributions to EOSC and Open Science 2023"),
            model("m-eosc-sb-2024", "Survey on National Contributions to EOSC and Open Science 2024")
        ));

        DimensionMembers page = provider.search(null, "2024", 100);

        assertThat(page.members()).containsExactly(
            new DimensionMember("m-eosc-sb-2024", "Survey on National Contributions to EOSC and Open Science 2024")
        );
    }

    @Test
    void truncatesToLimit() {
        when(surveyService.getByType(any(FacetFilter.class), eq("eosc-sb"))).thenReturn(browsingOf(
            model("m-eosc-sb-2023", "2023 survey"),
            model("m-eosc-sb-2024", "2024 survey"),
            model("m-eosc-sb-2025", "2025 survey")
        ));

        DimensionMembers page = provider.search(null, "", 2);

        assertThat(page.members()).hasSize(2);
    }

    private static Model model(String id, String name) {
        Model model = new Model();
        model.setId(id);
        model.setName(name);
        return model;
    }

    private static Browsing<Model> browsingOf(Model... models) {
        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(List.of(models));
        return browsing;
    }
}
