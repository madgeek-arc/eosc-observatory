package eu.openaire.observatory.dto;

import eu.openaire.observatory.domain.Editor;
import eu.openaire.observatory.domain.HistoryEntry;
import eu.openaire.observatory.domain.History;
import eu.openaire.observatory.domain.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryEntryDTOTest {

    @Test
    void ofProducesNullEmailEditorForLegacyRecordWithEmptyEditorsAndNullUserId() {
        HistoryEntry entry = new HistoryEntry();
        // editors is empty (default), userId is null (default) — simulates a very old persisted record
        entry.setTime(1000L);
        entry.setAction(History.HistoryAction.UPDATED);

        HistoryEntryDTO dto = HistoryEntryDTO.of(entry, anyUser());

        assertThat(dto.getEditors()).hasSize(1);
        assertThat(dto.getEditors().getFirst().getEmail()).isNull();
    }

    @Test
    void ofProducesNullEmailEditorForLegacyRecordWithNullEditorsList() {
        HistoryEntry entry = new HistoryEntry();
        entry.setEditors(null);
        entry.setTime(1000L);
        entry.setAction(History.HistoryAction.UPDATED);

        HistoryEntryDTO dto = HistoryEntryDTO.of(entry, anyUser());

        assertThat(dto.getEditors()).hasSize(1);
        assertThat(dto.getEditors().getFirst().getEmail()).isNull();
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
