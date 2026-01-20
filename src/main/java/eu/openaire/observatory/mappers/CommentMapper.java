/*
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.mappers;

import eu.openaire.observatory.commenting.domain.CommentThread;
import eu.openaire.observatory.commenting.domain.CommentMessage;
import eu.openaire.observatory.commenting.domain.MessageMention;
import eu.openaire.observatory.commenting.dto.CommentDto;
import eu.openaire.observatory.commenting.dto.CommentMessageDto;
import eu.openaire.observatory.commenting.dto.MentionDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    CommentDto toDto(CommentThread thread);

    CommentMessage toCommentMessage(CommentMessageDto dto);
    CommentMessageDto toDto(CommentMessage dto);

    MessageMention toMessageMention(MentionDto dto);
    MentionDto toDto(MessageMention dto);

}
