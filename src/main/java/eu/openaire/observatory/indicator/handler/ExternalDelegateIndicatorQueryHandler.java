package eu.openaire.observatory.indicator.handler;

import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;

public class ExternalDelegateIndicatorQueryHandler implements IndicatorQueryHandler {

    private final String indicatorCode;
    private final RemoteIndicatorClient client;

    public ExternalDelegateIndicatorQueryHandler(String indicatorCode, RemoteIndicatorClient client) {
        this.indicatorCode = indicatorCode;
        this.client = client;
    }

    @Override
    public String indicatorCode() {
        return indicatorCode;
    }

    @Override
    public IndicatorResult execute(IndicatorExecutionPlan plan) {
        return client.query(plan);
    }
}
