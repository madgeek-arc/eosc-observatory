package eu.eosc.observatory.dto;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HistoryDTO {

    List<HistoryEntryDTO> entries = new ArrayList<>();

    public HistoryDTO() {
        // no-arg constructor
    }

    public HistoryDTO(List<HistoryEntryDTO> entries) {
        this.entries = new LinkedList<>(entries);
    }

    public List<HistoryEntryDTO> getEntries() {
        return entries;
    }

    public void setEntries(List<HistoryEntryDTO> entries) {
        this.entries = entries;
    }
}
