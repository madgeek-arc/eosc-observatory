package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.SurveyAnswerRevisionsAggregation;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.catalogue.service.id.IdGenerator;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyAnswerCrudServiceTest {

    @Mock
    private ResourceTypeService resourceTypeService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private SearchService searchService;
    @Mock
    private VersionService versionService;
    @Mock
    private ParserService parserService;
    @Mock
    private IdGenerator<String> idGenerator;
    @Mock
    private CacheService<String, SurveyAnswerRevisionsAggregation> cacheService;
    @Mock
    private ModelResponseValidator validator;

    private SurveyAnswerCrudService service;

    @BeforeEach
    void setUp() {
        service = spy(new SurveyAnswerCrudService(
                resourceTypeService,
                resourceService,
                searchService,
                versionService,
                parserService,
                idGenerator,
                cacheService,
                validator
        ));
        service.setBrowseByMap(Map.of("survey_answer", List.of()));
        ReflectionTestUtils.setField(service, "labelsMap", Map.of("survey_answer", Map.of()));
    }

    @Test
    void addRejectsDuplicateAnswerPerSurveyAndStakeholder() {
        SurveyAnswer duplicate = createSurveyAnswer("survey-1", "stakeholder-1", "sa-1");
        Resource existingResource = new Resource();
        existingResource.setResourceTypeName("survey_answer");
        Paging<Resource> paging = new Paging<>();
        paging.setResults(List.of(existingResource));

        when(searchService.search(any(FacetFilter.class))).thenReturn(paging);
        when(resourceTypeService.getResourceType("survey_answer")).thenReturn(surveyAnswerResourceType());
        when(parserService.deserialize(existingResource, SurveyAnswer.class)).thenReturn(duplicate);

        assertThrows(ServiceException.class, () -> service.add(duplicate));
    }

    @Test
    void getReturnsCachedSurveyAnswerWhenPresent() {
        SurveyAnswer cachedAnswer = createSurveyAnswer("survey-1", "stakeholder-1", "sa-1");
        SurveyAnswerRevisionsAggregation cached = new SurveyAnswerRevisionsAggregation(cachedAnswer);
        when(cacheService.fetch("sa-1")).thenReturn(cached);

        SurveyAnswer result = service.get("sa-1");

        assertSame(cachedAnswer, result);
        verify(cacheService).fetch("sa-1");
    }

    @Test
    void updateFlushesCachedVersionBeforePersistingNewResource() throws ResourceNotFoundException {
        SurveyAnswer cachedAnswer = createSurveyAnswer("survey-1", "stakeholder-1", "sa-1");
        SurveyAnswer incoming = createSurveyAnswer("survey-1", "stakeholder-1", "sa-1");
        SurveyAnswerRevisionsAggregation cached = new SurveyAnswerRevisionsAggregation(cachedAnswer);
        ResourceType resourceType = surveyAnswerResourceType();
        Resource resource = new Resource();
        resource.setId("sa-1");

        when(cacheService.fetch("sa-1")).thenReturn(cached);
        when(resourceTypeService.getResourceType("survey_answer")).thenReturn(resourceType);
        when(searchService.searchFields(eq("survey_answer"), any(SearchService.KeyValue[].class))).thenReturn(resource);
        when(parserService.serialize(any(SurveyAnswer.class), any(ParserService.ParserServiceTypes.class))).thenReturn("{}");

        SurveyAnswer result = service.update("sa-1", incoming);

        assertSame(incoming, result);
        verify(resourceService, times(2)).updateResource(resource);
        verify(cacheService).remove("sa-1");
    }

    @Test
    void autoSaveCachePersistsStaleEntriesAndSkipsFailingOnes() throws ResourceNotFoundException {
        SurveyAnswer staleAnswer = createSurveyAnswer("survey-1", "stakeholder-1", "sa-1");
        SurveyAnswerRevisionsAggregation stale = new SurveyAnswerRevisionsAggregation(staleAnswer);
        stale.getCreated().setTime(System.currentTimeMillis() - 700_000L);

        when(cacheService.fetchKeys("sa-*")).thenReturn(Set.of("sa-1", "sa-2"));
        when(cacheService.fetch("sa-1")).thenReturn(stale);
        when(cacheService.fetch("sa-2")).thenThrow(new RuntimeException("broken"));
        doReturn(staleAnswer).when(service).update("sa-1", staleAnswer);

        service.autoSaveCache();

        verify(service).update("sa-1", staleAnswer);
        verify(cacheService).fetch("sa-2");
    }

    private ResourceType surveyAnswerResourceType() {
        ResourceType resourceType = new ResourceType();
        resourceType.setName("survey_answer");
        resourceType.setPayloadType("json");
        resourceType.setProperties(Map.of("class", SurveyAnswer.class.getName()));
        return resourceType;
    }

    private SurveyAnswer createSurveyAnswer(String surveyId, String stakeholderId, String id) {
        SurveyAnswer surveyAnswer = new SurveyAnswer();
        surveyAnswer.setId(id);
        surveyAnswer.setSurveyId(surveyId);
        surveyAnswer.setStakeholderId(stakeholderId);
        surveyAnswer.setAnswer(new JSONObject());
        surveyAnswer.getMetadata().setCreationDate(new Date());
        surveyAnswer.getMetadata().setModificationDate(new Date());
        return surveyAnswer;
    }
}
