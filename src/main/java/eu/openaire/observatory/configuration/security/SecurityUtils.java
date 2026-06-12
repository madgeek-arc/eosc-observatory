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

package eu.openaire.observatory.configuration.security;

import eu.openaire.observatory.service.Identifiable;
import gr.uoa.di.madgik.catalogue.utils.ReflectUtils;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;

class SecurityUtils {

    private SecurityUtils() {}

    static String getResourceId(Object resource, Logger logger) {
        if (resource instanceof String) {
            return resource.toString();
        } else if (resource instanceof Identifiable) {
            return ((Identifiable<String>) resource).getId();
        } else {
            try {
                return ReflectUtils.getId(resource.getClass(), resource);
            } catch (NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }
}
