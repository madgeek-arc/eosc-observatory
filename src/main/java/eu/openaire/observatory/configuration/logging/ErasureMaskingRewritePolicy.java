/*
 * Copyright 2021-2026 OpenAIRE AMKE
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

package eu.openaire.observatory.configuration.logging;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.util.StringMap;

/**
 * Replaces the {@code user} MDC value on a log event with {@code erased:<hmac>} when that user has
 * been erased ({@link ErasureLogMasking}). Living users' log lines pass through untouched; erased
 * users stay correlatable by their stable HMAC without their address sitting in the logs.
 *
 * <p>Only the structured {@code user} context-data field is touched — an address a caller wrote into
 * a log <em>message</em> is out of scope (see the GDPR erasure runbook).
 */
@Plugin(name = "ErasureMaskingRewritePolicy", category = Core.CATEGORY_NAME,
        elementType = "rewritePolicy", printObject = true)
public final class ErasureMaskingRewritePolicy implements RewritePolicy {

    private static final String USER_KEY = "user";

    private ErasureMaskingRewritePolicy() {
    }

    @Override
    public LogEvent rewrite(final LogEvent event) {
        if (event.getContextData() == null) {
            return event;
        }
        String user = event.getContextData().getValue(USER_KEY);
        if (user == null) {
            return event;
        }
        String masked = ErasureLogMasking.maskedReferenceFor(user);
        if (masked == null) {
            return event;
        }
        StringMap newContextData = ContextDataFactory.createContextData();
        event.getContextData().forEach(newContextData::putValue);
        newContextData.putValue(USER_KEY, masked);
        return new Log4jLogEvent.Builder(event).setContextData(newContextData).build();
    }

    @PluginFactory
    public static ErasureMaskingRewritePolicy createPolicy() {
        return new ErasureMaskingRewritePolicy();
    }

    @Override
    public String toString() {
        return "ErasureMaskingRewritePolicy";
    }
}
