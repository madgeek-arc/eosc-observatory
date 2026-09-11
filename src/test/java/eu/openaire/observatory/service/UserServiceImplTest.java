package eu.openaire.observatory.service;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.domain.Administrator;
import eu.openaire.observatory.domain.Coordinator;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.User;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ParserService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

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
    private PrivacyPolicyService privacyPolicyService;
    @Mock
    private CrudService<Stakeholder> stakeholderCrudService;
    @Mock
    private CrudService<Coordinator> coordinatorCrudService;
    @Mock
    private CrudService<Administrator> administratorCrudService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ModelResponseValidator validator;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
                resourceTypeService,
                resourceService,
                searchService,
                versionService,
                parserService,
                privacyPolicyService,
                stakeholderCrudService,
                coordinatorCrudService,
                administratorCrudService,
                applicationProperties,
                validator
        );
    }

    @Test
    void getThrowsResourceNotFoundForNullIdWithoutSearching() {
        assertThrows(ResourceNotFoundException.class, () -> service.get(null));
        verifyNoInteractions(searchService);
    }

    @Test
    void getThrowsResourceNotFoundForBlankIdWithoutSearching() {
        assertThrows(ResourceNotFoundException.class, () -> service.get(""));
        verifyNoInteractions(searchService);
    }

    @Test
    void getUserReturnsPlaceholderForNullId() {
        User user = service.getUser(null);

        assertNull(user.getEmail());
        verifyNoInteractions(searchService);
    }
}
