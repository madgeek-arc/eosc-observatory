package eu.openaire.observatory.commenting.dto;

import java.util.UUID;

public record MentionDto(
        String userId,
        UUID messageId
) {}
