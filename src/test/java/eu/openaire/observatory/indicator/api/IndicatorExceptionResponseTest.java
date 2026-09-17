package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.controller.advices.ExceptionControllerAdvice;
import eu.openaire.observatory.indicator.IndicatorQueryService;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.IndicatorAccessDeniedException;
import eu.openaire.observatory.indicator.validation.InvalidFilterShapeException;
import eu.openaire.observatory.indicator.validation.SeriesAggregationNotAllowedException;
import eu.openaire.observatory.indicator.validation.UnknownIndicatorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the HTTP mapping of the indicator validation exceptions: they must surface with their real
 * message and the right status, not be masked as {@code "Internal Server Error"} by
 * {@code GenericExceptionController}'s fallback branch.
 */
class IndicatorExceptionResponseTest {

    private final IndicatorQueryService queryService = Mockito.mock(IndicatorQueryService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        IndicatorQueryController controller =
            new IndicatorQueryController(queryService, Mockito.mock(ActorContext.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();
    }

    @Test
    void invalidFilterShape_isBadRequestWithMessage() throws Exception {
        when(queryService.execute(any(), any())).thenThrow(
            new InvalidFilterShapeException("country", "EQ requires exactly one value"));
        mockMvc.perform(post("/indicators/query").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid filter country: EQ requires exactly one value"));
    }

    @Test
    void seriesAggregationNotAllowed_isBadRequestWithMessage() throws Exception {
        when(queryService.execute(any(), any()))
            .thenThrow(new SeriesAggregationNotAllowedException("preset-1", AggregationType.SUM));

        mockMvc.perform(post("/indicators/query").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("does not allow the series aggregation")));
    }

    @Test
    void unknownIndicator_isNotFoundWithMessage() throws Exception {
        when(queryService.execute(any(), any())).thenThrow(new UnknownIndicatorException("foo"));

        mockMvc.perform(post("/indicators/query").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Unknown indicator: foo"));
    }

    @Test
    void indicatorAccessDenied_isForbiddenWithMessage() throws Exception {
        when(queryService.execute(any(), any())).thenThrow(new IndicatorAccessDeniedException("foo"));

        mockMvc.perform(post("/indicators/query").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Access denied for indicator: foo"));
    }
}
