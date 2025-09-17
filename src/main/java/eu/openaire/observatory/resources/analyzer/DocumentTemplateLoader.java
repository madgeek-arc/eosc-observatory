package eu.openaire.observatory.resources.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentTemplateLoader {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTemplateLoader.class);

    private JsonNode template;
    private final ObjectMapper mapper;

    public DocumentTemplateLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode load() {
        if (template == null) {
            try (InputStream is = DocumentTemplateLoader.class.getClassLoader().getResourceAsStream("template.json")) {

                if (is != null) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    logger.debug("JSON Template:\n{}", content);
                    this.template = mapper.readTree(content);
                } else {
                    logger.error("Could not read template.json");
                    throw new IOException("Could not read template.json");
                }
            } catch (IOException e) {
                return null;
            }
        }
        return template;
    }
}