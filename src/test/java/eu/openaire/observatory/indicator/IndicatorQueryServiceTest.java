package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.handler.ExternalDelegateIndicatorQueryHandler;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.handler.LocalAggregationIndicatorQueryHandler;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorMetadata;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.result.QueryExecutionMetadata;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.IndicatorDefinitionLookup;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.UnknownIndicatorException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the core hybrid-sourcing requirement: an indicator backed by local aggregation and one
 * delegated to an external service both flow through the same IndicatorQuery -> IndicatorResult
 * contract, via IndicatorQueryService, without either source leaking into that contract.
 */
class IndicatorQueryServiceTest {

    private static final String LOCAL_CODE = "local.count";
    private static final String EXTERNAL_CODE = "external.count";

    private final IndicatorDefinition localDefinition = IndicatorFixtures.countIndicator(LOCAL_CODE);
    private final IndicatorDefinition externalDefinition = IndicatorFixtures.countIndicator(EXTERNAL_CODE);

    private final IndicatorDefinitionLookup definitionLookup = code -> {
        if (LOCAL_CODE.equals(code)) return localDefinition;
        if (EXTERNAL_CODE.equals(code)) return externalDefinition;
        throw new UnknownIndicatorException(code);
    };

    private final eu.openaire.observatory.indicator.validation.DimensionDefinitionLookup dimensionLookup =
        code -> IndicatorFixtures.countryDimension();

    private final ActorContext allowAll = level -> true;

    @Test
    void localAndExternalHandlersProduceInterchangeableResultsThroughTheSameService() {
        IndicatorResult localCanned = cannedResult(LOCAL_CODE, 123);
        IndicatorResult externalCanned = cannedResult(EXTERNAL_CODE, 456);

        var localHandler = new LocalAggregationIndicatorQueryHandler(LOCAL_CODE, plan -> localCanned);
        var externalHandler = new ExternalDelegateIndicatorQueryHandler(EXTERNAL_CODE, plan -> externalCanned);

        var registry = new IndicatorQueryHandlerRegistry(List.of(localHandler, externalHandler));
        var validator = new IndicatorQueryValidator(definitionLookup, dimensionLookup);
        var service = new IndicatorQueryService(validator, registry, new TimeSeriesNormalizer());

        var localResult = service.execute(query(LOCAL_CODE), allowAll);
        var externalResult = service.execute(query(EXTERNAL_CODE), allowAll);

        assertThat(localResult.metadata().indicatorCode()).isEqualTo(LOCAL_CODE);
        assertThat(localResult.data().get(0).value()).isEqualTo(123);

        assertThat(externalResult.metadata().indicatorCode()).isEqualTo(EXTERNAL_CODE);
        assertThat(externalResult.data().get(0).value()).isEqualTo(456);
    }

    private static IndicatorQuery query(String code) {
        return new IndicatorQuery(code, null, List.of(), List.of(), null, List.of(), null);
    }

    private static IndicatorResult cannedResult(String code, int value) {
        return new IndicatorResult(
            new IndicatorMetadata(code, "label", null, null, null),
            List.of(),
            List.of(new IndicatorDataPoint(Map.of(), value)),
            new QueryExecutionMetadata(Instant.now(), false)
        );
    }
}
