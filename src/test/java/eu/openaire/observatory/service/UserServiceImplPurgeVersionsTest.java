package eu.openaire.observatory.service;

import eu.openaire.observatory.IntegrationTestConfig;
import eu.openaire.observatory.domain.SurveyAnswer;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.VersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceImplPurgeVersionsTest extends IntegrationTestConfig {

    private static final String USER_ID = "purge-versions-test@example.org";

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private CrudService<SurveyAnswer> surveyAnswerCrudService;

    @Autowired
    private VersionService versionService;

    @Test
    void purgeShouldRemoveUserFromSurveyAnswerVersionHistory() throws ResourceNotFoundException {
        // TODO: build a SurveyAnswer whose History has an entry with USER_ID as editor, and
        // whose Metadata.createdBy/modifiedBy is USER_ID, then persist it via
        // surveyAnswerCrudService.add(...).

        // TODO: call surveyAnswerCrudService.update(...) once (e.g. touch an unrelated field)
        // so the registry framework snapshots the pre-update payload — still containing the
        // raw USER_ID — into a ResourceVersion row. Confirm via versionService.getVersionsByResource(id)
        // that at least one version now exists.

        // TODO: call userService.purge(USER_ID). This only rewrites the *current* SurveyAnswer
        // row (see the TODO comments in UserServiceImpl#purge) — the version snapshotted above
        // is untouched.

        // TODO: re-fetch the version history (versionService.getVersionsByResource(answerId), or
        // surveyAnswerCrudService.getHistory(answerId, transform)) and assert USER_ID does not
        // appear in any historical entry's payload (editor/createdBy/modifiedBy). This is expected
        // to FAIL today — purge() does not implement registry-core-version erasure yet.

        fail("TODO: implement — see UserServiceImpl#purge TODO comments re: registry core versions");
    }
}
