package eu.openaire.observatory.dto;

import eu.openaire.observatory.domain.Editor;
import eu.openaire.observatory.domain.HistoryEntry;
import eu.openaire.observatory.domain.History;
import eu.openaire.observatory.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryEntryDTOTest {

    @Test
    void ofLeavesEditorsEmptyForUnresolvedEntryWithEmptyEditorsAndNullUserId() {
        HistoryEntry entry = new HistoryEntry();
        // editors is empty (default), userId is null (default) — simulates an unresolved/placeholder record
        entry.setTime(1000L);
        entry.setAction(History.HistoryAction.UPDATED);

        HistoryEntryDTO dto = HistoryEntryDTO.of(entry, anyUser());

        // no editor is fabricated, so no fake "unknown" editor / date can be rendered
        assertThat(dto.getEditors()).isEmpty();
    }

    @Test
    void ofLeavesEditorsEmptyForUnresolvedEntryWithNullEditorsList() {
        HistoryEntry entry = new HistoryEntry();
        entry.setEditors(null);
        entry.setTime(1000L);
        entry.setAction(History.HistoryAction.UPDATED);

        HistoryEntryDTO dto = HistoryEntryDTO.of(entry, anyUser());

        assertThat(dto.getEditors()).isEmpty();
    }

    @Test
    void ofProducesNullEmailEditorForLegacyRecordWithUserIdButNoEditors() {
        HistoryEntry entry = new HistoryEntry();
        // old-style record: no "editors" list, but the deprecated userId/userRole fields are set
        entry.setUserId("legacy-but-unresolvable-id");
        entry.setUserRole("manager");
        entry.setTime(1000L);
        entry.setAction(History.HistoryAction.UPDATED);

        HistoryEntryDTO dto = HistoryEntryDTO.of(entry, anyUser());

        assertThat(dto.getEditors()).hasSize(1);
        assertThat(dto.getEditors().getFirst().getEmail()).isEqualTo("legacy-but-unresolvable-id");
    }

    @Test
    void ofMapsRealisticEpochMillisTimeWithoutOverflowingIntoFarFutureDate() {
        HistoryEntry entry = new HistoryEntry();
        long realisticEpochMillis = System.currentTimeMillis();
        entry.setUserId("editor@example.org");
        entry.setUserRole("administrator");
        entry.setTime(realisticEpochMillis);
        entry.setAction(History.HistoryAction.UPDATED);

        HistoryEntryDTO dto = HistoryEntryDTO.of(entry, anyUser());

        assertThat(dto.getEditors()).hasSize(1);
        assertThat(dto.getEditors().getFirst().getUpdateDate())
                .isEqualTo(Date.from(Instant.ofEpochMilli(realisticEpochMillis)));
    }

    @Test
    void ofMapsEditorEmailFromModernRecord() {
        HistoryEntry entry = new HistoryEntry();
        entry.setEditors(List.of(new Editor("alice@example.com", "manager")));
        entry.setTime(1000L);
        entry.setAction(History.HistoryAction.UPDATED);

        HistoryEntryDTO dto = HistoryEntryDTO.of(entry, anyUser());

        assertThat(dto.getEditors()).hasSize(1);
        assertThat(dto.getEditors().getFirst().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void ofMapsMultipleEditorsFromModernRecord() {
        HistoryEntry entry = new HistoryEntry();
        entry.setEditors(List.of(
                new Editor("alice@example.com", "manager"),
                new Editor("bob@example.com", "contributor")
        ));
        entry.setTime(1000L);
        entry.setAction(History.HistoryAction.UPDATED);

        HistoryEntryDTO dto = HistoryEntryDTO.of(entry, anyUser());

        assertThat(dto.getEditors()).extracting(EditorDTO::getEmail)
                .containsExactly("alice@example.com", "bob@example.com");
    }

    private static User anyUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setFullname("Test User");
        return user;
    }
}
