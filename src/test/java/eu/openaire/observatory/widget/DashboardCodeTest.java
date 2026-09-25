package eu.openaire.observatory.widget;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashboardCodeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void wireValue_isKebabCase() {
        assertEquals("country-pages", DashboardCode.COUNTRY_PAGES.wireValue());
    }

    @Test
    void fromWireValue_resolvesKnownCode() {
        assertSame(DashboardCode.COUNTRY_PAGES, DashboardCode.fromWireValue("country-pages"));
    }

    @Test
    void fromWireValue_rejectsUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> DashboardCode.fromWireValue("typo-pages"));
        assertThrows(IllegalArgumentException.class, () -> DashboardCode.fromWireValue(null));
    }

    @Test
    void defaults_serializeCodeAsWireValue() throws Exception {
        DashboardDefaults defaults = new DashboardDefaults();
        defaults.setCode(DashboardCode.COUNTRY_PAGES);

        assertTrue(mapper.writeValueAsString(defaults).contains("\"code\":\"country-pages\""));
    }

    @Test
    void overrides_deserializeCodeFromWireValue() throws Exception {
        DashboardOverrides overrides = mapper.readValue("{\"code\":\"country-pages\"}", DashboardOverrides.class);

        assertSame(DashboardCode.COUNTRY_PAGES, overrides.getCode());
    }
}
