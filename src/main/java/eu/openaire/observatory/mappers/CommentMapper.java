package eu.openaire.observatory.mappers;

import eu.openaire.observatory.commenting.domain.Comment;
import eu.openaire.observatory.commenting.domain.CommentMessage;
import eu.openaire.observatory.commenting.domain.MessageMention;
import eu.openaire.observatory.commenting.dto.CommentDto;
import eu.openaire.observatory.commenting.dto.CommentMessageDto;
import eu.openaire.observatory.commenting.dto.CreateComment;
import eu.openaire.observatory.commenting.dto.MentionDto;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    default Comment toComment(CommentDto dto) {
        Comment comment = new Comment();
        comment.setTarget(dto.target());
        comment.setFieldId(dto.fieldId());
        comment.setStatus(dto.status());
        List<CommentMessage> messages = new ArrayList<>();
        for (CommentMessageDto dtoMsg : dto.messages()) {
            CommentMessage message = toCommentMessage(dtoMsg);
            message.setComment(comment);

            messages.add(message);
            for(MessageMention mention : message.getMentions()) {
                mention.setMessageId(message.getId());
            }
        }
        comment.setMessages(messages);
        return comment;
    }

    Comment toComment(CreateComment dto);

    CommentDto toDto(Comment dto);

    CommentMessage toCommentMessage(CommentMessageDto dto);
    CommentMessageDto toDto(CommentMessage dto);

    MessageMention toMessageMention(MentionDto dto);
    MentionDto toDto(MessageMention dto);

}
