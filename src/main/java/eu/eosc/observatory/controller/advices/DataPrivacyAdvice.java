package eu.eosc.observatory.controller.advices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.eosc.observatory.configuration.PrivacyProperties;
import eu.eosc.observatory.configuration.security.MethodSecurityExpressions;
import eu.eosc.observatory.dto.IdField;
import eu.eosc.observatory.service.SecurityService;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@ControllerAdvice
public class DataPrivacyAdvice<T> implements ResponseBodyAdvice<T> {

    private static final Logger logger = LogManager.getLogger(DataPrivacyAdvice.class);
    private final SecurityService securityService;
    private final MethodSecurityExpressions securityExpressions;
    private final PrivacyProperties privacyProperties;
    private final PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final List<String> classNames;


    public DataPrivacyAdvice(SecurityService securityService,
                             MethodSecurityExpressions securityExpressions,
                             PrivacyProperties privacyProperties) {
        this.securityService = securityService;
        this.securityExpressions = securityExpressions;
        this.privacyProperties = privacyProperties;
        this.classNames = privacyProperties.getEntries()
                .stream()
                .map(PrivacyProperties.FieldPrivacy::getClassName)
                .toList();
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public T beforeBodyWrite(T body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        T ret = null;
        if (body != null && classNames.contains(body.getClass().getCanonicalName())) {

            try {
                String id = getId(body);
                if (auth instanceof AnonymousAuthenticationToken || (!securityExpressions.isAdmin(auth) && !securityService.canRead(auth, id))) {
                    logger.trace("User lacks read permission : removing sensitive information");

                    // transform body to json and convert back to T (deep copy)
                    String json = mapper.writeValueAsString(body);
                    ret = (T) mapper.readValue(json, body.getClass());

                    // apply content transformations
                    modifyContent(ret);

                    logger.trace("Final Object: {}", json);
                }
            } catch (JsonProcessingException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return ret == null ? body : ret;
    }

    /**
     * Uses {@link PrivacyProperties} to filter out sensitive information in the provided object.
     *
     * @param obj
     */
    public void modifyContent(T obj) {
        if (obj != null) {
            Class<?> clazz = obj.getClass();

            for (PrivacyProperties.FieldPrivacy fieldPrivacy : privacyProperties.getEntries()) {
                if (clazz.getCanonicalName().equals(fieldPrivacy.getClassName())) {

                    try {
                        logger.debug("Removing non-public Data");
                        replaceData(obj, fieldPrivacy.getField(), null);
                    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                        throw new ResourceException(e, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }
    }

    /**
     * Accepts an {@link java.lang.Object} and uses recursion to replace its data on the specified field-path.
     *
     * @param obj
     * @param field
     * @param value
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     */
    public void replaceData(Object obj, String field, Object value) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (field.contains("[].")) {
            String[] fieldPath = field.split("\\[\\]\\.", 2);
            List<?> items = (List<?>) propertyUtilsBean.getProperty(obj, fieldPath[0]);
            if (items != null) {
                for (Object item : items) {
                    replaceData(item, fieldPath[1], value);
                }
            }
        } else {
            propertyUtilsBean.setProperty(obj, field, value);
        }
    }

    /**
     * This method is a workaround to get the ID field of an object that does not have a declared id field.
     * e.g. {@link java.util.Map}, {@link org.json.simple.JSONObject}
     *
     * @param obj The object whose ID must be found
     * @return the id
     */
    public String getId(Object obj) {
        // TODO:
        //  1. check what happens in nested objects with multiple id fields.
        //  2. replace functionality with cast to Map (if possible) and access through 'get("id")'.
        //  3. replace return type from String to Object ?
        IdField id = mapper.convertValue(obj, IdField.class);
        if (id == null) {
            throw new RuntimeException("ID field is null : class = " + obj.getClass().getCanonicalName());
        }
        return id.getId();
    }
}