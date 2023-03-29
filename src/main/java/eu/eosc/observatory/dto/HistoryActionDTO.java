package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.History;

public class HistoryActionDTO {

    private History.HistoryAction type;
    private String registryVersion;
    private HistoryEntryDTO pointsTo;

    public HistoryActionDTO() {
    }

    public static HistoryActionDTO of(History.HistoryAction type, String registryVersion) {
        HistoryActionDTO actionDTO = new HistoryActionDTO();
        actionDTO.setType(type);
        actionDTO.setRegistryVersion(registryVersion);
        return actionDTO;
    }

    public History.HistoryAction getType() {
        return type;
    }

    public void setType(History.HistoryAction type) {
        this.type = type;
    }

    public String getRegistryVersion() {
        return registryVersion;
    }

    public void setRegistryVersion(String registryVersion) {
        this.registryVersion = registryVersion;
    }

    public HistoryEntryDTO getPointsTo() {
        return pointsTo;
    }

    public void setPointsTo(HistoryEntryDTO pointsTo) {
        this.pointsTo = pointsTo;
    }
}
