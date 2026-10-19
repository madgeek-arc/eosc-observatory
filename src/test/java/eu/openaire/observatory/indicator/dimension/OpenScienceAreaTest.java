package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenScienceAreaTest {

    @Test
    void thereAreTwelveAreas() {
        assertThat(OpenScienceArea.values()).hasSize(12);
    }

    @Test
    void everyAreaHasAnInvestmentQuestionAndOneQuestionPerInitiativeKind() {
        for (OpenScienceArea area : OpenScienceArea.values()) {
            assertThat(area.investmentQuestion()).startsWith("Question");
            for (OpenScienceArea.InitiativeKind kind : OpenScienceArea.InitiativeKind.values()) {
                assertThat(area.initiativeQuestion(kind)).as(area + " " + kind).startsWith("Question");
            }
        }
    }

    @Test
    void noQuestionCodeIsUsedByTwoAreaCells() {
        var seen = new HashSet<String>();
        for (OpenScienceArea area : OpenScienceArea.values()) {
            assertThat(seen.add(area.investmentQuestion())).as(area.investmentQuestion()).isTrue();
            for (OpenScienceArea.InitiativeKind kind : OpenScienceArea.InitiativeKind.values()) {
                assertThat(seen.add(area.initiativeQuestion(kind))).as(area.initiativeQuestion(kind)).isTrue();
            }
        }
        assertThat(seen).hasSize(48);
    }

    @Test
    void initiativeQuestionCodesCoverTheGridAndImmediateOaPolicy() {
        Map<String, String> codes = OpenScienceArea.initiativeQuestionCodes();

        assertThat(codes).hasSize(37)
            .containsEntry("OA_PUBLICATION_POLICY", "Question6")
            .containsEntry("IMMEDIATE_OA_POLICY", "Question6.3")
            .containsEntry("OA_PUBLICATION_FINANCIAL_STRATEGY", "Question7")
            .containsEntry("OA_PUBLICATION_MONITORING_INITIATIVE", "Question54")
            .containsEntry("SERVICES_POLICY", "Question26")
            .containsEntry("REPOSITORIES_PRESERVATION_FINANCIAL_STRATEGY", "Question39");
    }

    @Test
    void initiativeTypeLabelIsTheAreaLabelForEveryKind() {
        for (OpenScienceArea.InitiativeKind kind : OpenScienceArea.InitiativeKind.values()) {
            assertThat(OpenScienceArea.OPEN_DATA.initiativeTypeLabel(kind)).isEqualTo("Open Data");
            assertThat(OpenScienceArea.SERVICES.initiativeTypeLabel(kind)).isEqualTo("Services");
        }
    }
}
