package eu.openaire.observatory.commenting.dto;

import java.util.List;

public record CommentMessageDto(
        String body,
        String authorId,
        List<MentionDto> mentions
) {
}
