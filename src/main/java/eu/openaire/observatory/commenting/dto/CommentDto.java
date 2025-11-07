package eu.openaire.observatory.commenting.dto;

import eu.openaire.observatory.commenting.domain.CommentStatus;
import eu.openaire.observatory.commenting.domain.CommentTarget;

import java.util.List;

public record CommentDto(
        CommentTarget target,
        String fieldId,
        CommentStatus status,
        List<CommentMessageDto> messages
) {
}
