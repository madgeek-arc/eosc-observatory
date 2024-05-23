/*
 * Copyright 2024 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.eosc.observatory.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import eu.eosc.observatory.domain.*;
import eu.eosc.observatory.service.SurveyAnswerCrudService;
import eu.eosc.observatory.service.SurveyService;
import eu.eosc.observatory.service.UserService;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ConcurrentEditService {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentEditService.class);

    private RedisTemplate<String, String> template;

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final SurveyAnswerCrudService surveyService;

//    // inject the template as ListOperations
    @Resource(name="redisTemplate")
    private ListOperations<String, SurveyAnswerRevisionsAggregation> listOps;


    public ConcurrentEditService(RedisTemplate<String, String> template, ObjectMapper objectMapper,
                                 UserService userService, SurveyAnswerCrudService surveyService) {
        this.template = template;
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.surveyService = surveyService;
    }

    void edit(String surveyAnswerId, Revision revision, Authentication authentication) {
        SurveyAnswerRevisionsAggregation sara = get(surveyAnswerId);
        sara.applyRevision(revision);
        save(sara);
    }

    private void save(SurveyAnswerRevisionsAggregation sara) {
        // TODO: save to cache
        listOps.leftPush(sara.getSurveyAnswer().getId(), sara);
    }

    private SurveyAnswerRevisionsAggregation get(String surveyAnswerId) {
        // TODO: find survey answer in Redis Cache or fetch it from db (only if not validated)
        SurveyAnswerRevisionsAggregation sara = listOps.leftPop(surveyAnswerId);
        if (sara == null) {
            sara = new SurveyAnswerRevisionsAggregation(surveyService.get(surveyAnswerId));
        }
        return sara;
    }


}
