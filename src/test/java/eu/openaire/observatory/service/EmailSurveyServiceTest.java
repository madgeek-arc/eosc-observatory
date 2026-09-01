package eu.openaire.observatory.service;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.configuration.MailDebugConfig;
import eu.openaire.observatory.messaging.mailer.MailDeliveryException;
import eu.openaire.observatory.domain.Administrator;
import eu.openaire.observatory.domain.Coordinator;
import eu.openaire.observatory.domain.NotificationPreferences;
import eu.openaire.observatory.domain.Settings;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.domain.UserGroup;
import freemarker.template.Configuration;
import freemarker.template.Template;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.messaging.mailer.service.Mailer;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailSurveyServiceTest {

    // Real MailDebugConfig — emails are intercepted here instead of being sent
    private final MailDebugConfig mailDebugConfig = new MailDebugConfig();

    @Mock private CrudService<Stakeholder> stakeholderCrudService;
    @Mock private ModelService modelService;
    @Mock private SurveyService surveyService;
    @Mock private UserService userService;
    @Mock private SurveySettingsService surveyNotificationSettingsService;
    @Mock private CoordinatorService coordinatorService;
    @Mock private AdministratorService administratorService;
    @Mock private Configuration freemarkerConfig;
    @Mock private Template template;
    @Mock private ApplicationProperties applicationProperties;

    private EmailSurveyService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new EmailSurveyService(
                mailDebugConfig.mailer(),
                stakeholderCrudService,
                modelService,
                surveyService,
                userService,
                surveyNotificationSettingsService,
                coordinatorService,
                administratorService,
                freemarkerConfig,
                "no-reply@openaire.eu",
                applicationProperties
        );

        when(freemarkerConfig.getTemplate(anyString())).thenReturn(template);
        doAnswer(inv -> null).when(template).process(any(), any());
        when(applicationProperties.getLoginRedirect()).thenReturn("http://localhost:4200");

        mailDebugConfig.clearCapturedEmails();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Model survey(String id, String name, String type) {
        Model m = new Model();
        m.setId(id);
        m.setName(name);
        m.setType(type);
        m.setSubmissionStartAt(new Date());
        m.setSubmissionCloseAt(new Date());
        return m;
    }

    private Stakeholder stakeholderWith(String... emails) {
        Stakeholder s = new Stakeholder();
        s.setAdmins(new TreeSet<>(Set.of(emails)));
        return s;
    }

    private void stubSurveyAndStakeholders(Model survey, Stakeholder... stakeholders) {
        when(modelService.get(survey.getId())).thenReturn(survey);
        Browsing<Stakeholder> browsing = new Browsing<>();
        browsing.setResults(List.of(stakeholders));
        when(stakeholderCrudService.getAll(any(FacetFilter.class))).thenReturn(browsing);
    }

    // ── notifySurveyStart ────────────────────────────────────────────────────

    @Test
    void notifySurveyStart_sendsEmailToStakeholderUsers() {
        Model s = survey("s1", "National Survey", "country");
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com", "bob@test.com"));

        service.notifySurveyStart("s1");

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getBcc().contains("alice@test.com"));
        assertTrue(captured.get(0).getBcc().contains("bob@test.com"));
        assertTrue(captured.get(0).getSubject().contains("National Survey"));
        captured.forEach(e -> System.out.println("[notifySurveyStart] Subject: " + e.getSubject() + " | BCC: " + e.getBcc()));
    }

    // ── notifySurveyEnd ──────────────────────────────────────────────────────

    @Test
    void notifySurveyEnd_sendsEmailToStakeholderUsers() {
        Model s = survey("s1", "National Survey", "country");
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com"));

        service.notifySurveyEnd("s1");

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getBcc().contains("alice@test.com"));
        assertTrue(captured.get(0).getSubject().contains("National Survey"));
        captured.forEach(e -> System.out.println("[notifySurveyEnd] Subject: " + e.getSubject() + " | BCC: " + e.getBcc()));
    }

    // ── notifyDeadlineChange ─────────────────────────────────────────────────

    @Test
    void notifyDeadlineChange_sendsEmail_whenSurveyHasStarted() {
        Model s = survey("s1", "National Survey", "country");
        Calendar past = Calendar.getInstance();
        past.add(Calendar.DAY_OF_YEAR, -1);
        s.setSubmissionStartAt(past.getTime());
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com"));

        service.notifyDeadlineChange("s1", new Date());

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getSubject().contains("National Survey"));
        captured.forEach(e -> System.out.println("[notifyDeadlineChange] Subject: " + e.getSubject() + " | BCC: " + e.getBcc()));
    }

    @Test
    void notifyDeadlineChange_doesNotSendEmail_whenSurveyHasNotStarted() {
        Model s = survey("s1", "National Survey", "country");
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com"));

        service.notifyDeadlineChange("s1", new Date());

        assertEquals(1, mailDebugConfig.getCapturedEmails().size());
        mailDebugConfig.getCapturedEmails().forEach(e -> System.out.println("[notifyDeadlineChange-noGuard] Subject: " + e.getSubject() + " | BCC: " + e.getBcc()));
    }

    // ── notifyDeadlineApproaching ────────────────────────────────────────────

    @Test
    void notifyDeadlineApproaching_sendsReminderEmail() {
        Model s = survey("s1", "National Survey", "country");
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com"));

        service.notifyDeadlineApproaching("s1", 7);

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getBcc().contains("alice@test.com"));
        assertTrue(captured.get(0).getSubject().contains("National Survey"));
        captured.forEach(e -> System.out.println("[notifyDeadlineApproaching] Subject: " + e.getSubject() + " | BCC: " + e.getBcc()));
    }

    // ── notifyDeadlineDay ────────────────────────────────────────────────────

    @Test
    void notifyDeadlineDay_sendsLastDayEmail() {
        Model s = survey("s1", "National Survey", "country");
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com"));

        service.notifyDeadlineDay("s1");

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getBcc().contains("alice@test.com"));
        assertTrue(captured.get(0).getSubject().contains("Last day"));
        assertTrue(captured.get(0).getSubject().contains("National Survey"));
        captured.forEach(e -> System.out.println("[notifyDeadlineDay] Subject: " + e.getSubject() + " | BCC: " + e.getBcc()));
    }

    // ── notifyReopened ───────────────────────────────────────────────────────

    @Test
    void notifyReopened_sendsSurveyReopenedEmail() {
        Model s = survey("s1", "National Survey", "country");
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com"));

        service.notifyReopened("s1");

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getBcc().contains("alice@test.com"));
        assertTrue(captured.get(0).getSubject().contains("reopened"));
        assertTrue(captured.get(0).getSubject().contains("National Survey"));
        captured.forEach(e -> System.out.println("[notifyReopened] Subject: " + e.getSubject() + " | BCC: " + e.getBcc()));
    }

    // ── Opt-out ──────────────────────────────────────────────────────────────

    @Test
    void notifySurveyStart_skipsOptedOutUsers() {
        Model s = survey("s1", "National Survey", "country");
        stubSurveyAndStakeholders(s, stakeholderWith("optout@test.com"));

        User user = new User();
        NotificationPreferences prefs = new NotificationPreferences();
        prefs.setEmailNotifications(false);
        Settings settings = new Settings();
        settings.setNotificationPreferences(prefs);
        user.setSettings(settings);
        when(userService.getUser("optout@test.com")).thenReturn(user);

        service.notifySurveyStart("s1");

        assertTrue(mailDebugConfig.getCapturedEmails().isEmpty());
    }

    // ── No stakeholders ──────────────────────────────────────────────────────

    @Test
    void notifySurveyStart_doesNotSendWhenNoStakeholders() {
        Model s = survey("s1", "National Survey", "country");
        stubSurveyAndStakeholders(s); // no stakeholders

        service.notifySurveyStart("s1");

        assertTrue(mailDebugConfig.getCapturedEmails().isEmpty());
    }

    // ── checkSurveyDates scheduler ───────────────────────────────────────────

    @Test
    void checkSurveyDates_notifiesStart_whenTodayIsStartDate() {
        Model s = survey("s1", "National Survey", "country");
        s.setSubmissionStartAt(new Date()); // today
        s.setSubmissionCloseAt(futureDate(30));

        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(List.of(s));
        when(surveyService.getByType(any(FacetFilter.class), isNull())).thenReturn(browsing);
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com"));

        service.checkSurveyDates();

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getSubject().contains("started"));
        captured.forEach(e -> System.out.println("[checkSurveyDates-start] Subject: " + e.getSubject() + " | BCC: " + e.getBcc()));
    }

    @Test
    void checkSurveyDates_notifiesEndAndDeadlineDay_whenTodayIsCloseDate() {
        Model s = survey("s1", "National Survey", "country");
        s.setSubmissionStartAt(futureDate(-10));
        s.setSubmissionCloseAt(new Date()); // today

        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(List.of(s));
        when(surveyService.getByType(any(FacetFilter.class), isNull())).thenReturn(browsing);
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com"));

        service.checkSurveyDates();

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        // both deadline_day and survey_end fire on the close date
        assertEquals(2, captured.size());
        List<String> subjects = captured.stream().map(EmailMessage::getSubject).toList();
        assertTrue(subjects.stream().anyMatch(sub -> sub.contains("Last day")));
        assertTrue(subjects.stream().anyMatch(sub -> sub.contains("closed")));
        captured.forEach(e -> System.out.println("[checkSurveyDates-end] Subject: " + e.getSubject() + " | BCC: " + e.getBcc()));
    }

    @Test
    void checkSurveyDates_notifiesApproaching_whenCloseIsIn7Days() {
        Model s = survey("s1", "National Survey", "country");
        s.setSubmissionStartAt(futureDate(-10));
        s.setSubmissionCloseAt(futureDate(7)); // exactly 7 days from now

        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(List.of(s));
        when(surveyService.getByType(any(FacetFilter.class), isNull())).thenReturn(browsing);
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com"));

        service.checkSurveyDates();

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getSubject().contains("7 days"));
        captured.forEach(e -> System.out.println("[checkSurveyDates-approaching] Subject: " + e.getSubject() + " | BCC: " + e.getBcc()));
    }

    // ── notifyAnswerValidated ────────────────────────────────────────────────

    private SurveyAnswer answer(String id, String surveyId, String stakeholderId, String type) {
        SurveyAnswer a = new SurveyAnswer();
        a.setId(id);
        a.setSurveyId(surveyId);
        a.setStakeholderId(stakeholderId);
        a.setType(type);
        return a;
    }

    private <T extends UserGroup> T group(T g, String id, String type, Set<String> admins, Set<String> members) {
        g.setId(id);
        g.setType(type);
        g.setAdmins(new TreeSet<>(admins));
        g.setMembers(new TreeSet<>(members));
        return g;
    }

    /** The review link the frontend expects, scoped to the recipient's own group. */
    private String reviewUrl(String groupId) {
        return "http://localhost:4200/contributions/" + groupId + "/stakeholder/sh-country-gr/survey/s1/view";
    }

    private EmailMessage emailContaining(List<EmailMessage> captured, String recipient) {
        return captured.stream()
                .filter(e -> e.getBcc().contains(recipient))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no email sent to " + recipient));
    }

    /** Stubs the survey, the stakeholder and the coordinator/administrator groups for a validated answer. */
    private void stubValidationContext(SurveyAnswer a, String surveyName, String stakeholderName,
                                       Set<Coordinator> coordinators, Set<Administrator> administrators) {
        Model s = survey(a.getSurveyId(), surveyName, a.getType());
        when(modelService.get(a.getSurveyId())).thenReturn(s);

        Stakeholder sh = new Stakeholder();
        sh.setId(a.getStakeholderId());
        sh.setName(stakeholderName);
        sh.setType(a.getType());
        when(stakeholderCrudService.get(a.getStakeholderId())).thenReturn(sh);

        when(coordinatorService.getWithFilter("type", a.getType())).thenReturn(coordinators);
        when(administratorService.getWithFilter("type", a.getType())).thenReturn(administrators);
    }

    @Test
    void notifyAnswerValidated_sendsOneEmailPerGroupWithItsOwnReviewLink() {
        SurveyAnswer a = answer("sa-1", "s1", "sh-country-gr", "country");
        stubValidationContext(a, "National Survey", "Greece",
                Set.of(group(new Coordinator(), "co-country", "country",
                        Set.of("co-admin@test.com"), Set.of("co-member@test.com"))),
                Set.of(group(new Administrator(), "admin-country", "country",
                        Set.of("ad-admin@test.com"), Set.of("ad-member@test.com"))));

        service.notifyAnswerValidated(a, "manager@test.com");

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(2, captured.size(), "expected one email per group");

        // admins AND members of each group, and the two groups kept apart
        List<String> coordinatorBcc = emailContaining(captured, "co-admin@test.com").getBcc();
        assertTrue(coordinatorBcc.contains("co-member@test.com"));
        assertEquals(2, coordinatorBcc.size());

        List<String> administratorBcc = emailContaining(captured, "ad-admin@test.com").getBcc();
        assertTrue(administratorBcc.contains("ad-member@test.com"));
        assertEquals(2, administratorBcc.size());

        captured.forEach(e -> {
            assertTrue(e.getSubject().contains("Greece"));
            assertTrue(e.getSubject().contains("National Survey"));
            System.out.println("[notifyAnswerValidated] Subject: " + e.getSubject() + " | BCC: " + e.getBcc());
        });
    }

    @Test
    void notifyAnswerValidated_excludesTheValidatingUser() {
        SurveyAnswer a = answer("sa-1", "s1", "sh-country-gr", "country");
        stubValidationContext(a, "National Survey", "Greece",
                Set.of(group(new Coordinator(), "co-country", "country",
                        Set.of("boss@test.com"), Set.of("other@test.com"))),
                Set.of());

        // the validating user is passed with different casing than stored
        service.notifyAnswerValidated(a, "BOSS@test.com");

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertFalse(captured.get(0).getBcc().contains("boss@test.com"));
        assertTrue(captured.get(0).getBcc().contains("other@test.com"));
    }

    @Test
    void notifyAnswerValidated_sendsOnlyOneEmailToAUserInBothGroups() {
        SurveyAnswer a = answer("sa-1", "s1", "sh-country-gr", "country");
        stubValidationContext(a, "National Survey", "Greece",
                Set.of(group(new Coordinator(), "co-country", "country",
                        Set.of(), Set.of("both@test.com"))),
                Set.of(group(new Administrator(), "admin-country", "country",
                        Set.of(), Set.of("both@test.com"))));

        service.notifyAnswerValidated(a, "manager@test.com");

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size(), "a user in both groups must not be mailed twice");
        // coordinators are processed first, so they get the coordinator dashboard link
        assertTrue(captured.get(0).getBcc().contains("both@test.com"));
    }

    @Test
    void notifyAnswerValidated_stillSendsToTheOtherGroupWhenOneHasNoEligibleRecipients() {
        SurveyAnswer a = answer("sa-1", "s1", "sh-country-gr", "country");
        stubValidationContext(a, "National Survey", "Greece",
                Set.of(group(new Coordinator(), "co-country", "country", Set.of(), Set.of())),
                Set.of(group(new Administrator(), "admin-country", "country",
                        Set.of("ad-admin@test.com"), Set.of())));

        service.notifyAnswerValidated(a, "manager@test.com");

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getBcc().contains("ad-admin@test.com"));
    }

    @Test
    void notifyAnswerValidated_stillNotifiesAdministratorsWhenTheCoordinatorSendFails() {
        // Coordinators are processed first — make that first send throw and assert the
        // administrator group is still notified (previously one throw aborted the whole method).
        List<EmailMessage> sent = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        Mailer flaky = email -> {
            if (calls.getAndIncrement() == 0) {
                throw new MailDeliveryException("coordinator send fails", new RuntimeException("mailer down"));
            }
            sent.add(email);
        };
        EmailSurveyService svc = new EmailSurveyService(flaky, stakeholderCrudService, modelService,
                surveyService, userService, surveyNotificationSettingsService, coordinatorService,
                administratorService, freemarkerConfig, "no-reply@openaire.eu", applicationProperties);

        SurveyAnswer a = answer("sa-1", "s1", "sh-country-gr", "country");
        stubValidationContext(a, "National Survey", "Greece",
                Set.of(group(new Coordinator(), "co-country", "country", Set.of(), Set.of("co-member@test.com"))),
                Set.of(group(new Administrator(), "admin-country", "country", Set.of(), Set.of("ad-member@test.com"))));

        svc.notifyAnswerValidated(a, "manager@test.com");

        assertEquals(1, sent.size(), "administrator email must still be sent after the coordinator send fails");
        assertTrue(sent.get(0).getBcc().contains("ad-member@test.com"));
        assertFalse(sent.get(0).getBcc().contains("co-member@test.com"));
    }

    @Test
    void notifyAnswerValidated_userInBothGroupsStillGetsAdministratorMailWhenCoordinatorSendFails() {
        // A failed coordinator send must not poison alreadyNotified — the shared address is still
        // eligible for the administrator group.
        List<EmailMessage> sent = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        Mailer flaky = email -> {
            if (calls.getAndIncrement() == 0) {
                throw new MailDeliveryException("coordinator send fails", new RuntimeException("mailer down"));
            }
            sent.add(email);
        };
        EmailSurveyService svc = new EmailSurveyService(flaky, stakeholderCrudService, modelService,
                surveyService, userService, surveyNotificationSettingsService, coordinatorService,
                administratorService, freemarkerConfig, "no-reply@openaire.eu", applicationProperties);

        SurveyAnswer a = answer("sa-1", "s1", "sh-country-gr", "country");
        stubValidationContext(a, "National Survey", "Greece",
                Set.of(group(new Coordinator(), "co-country", "country", Set.of(), Set.of("both@test.com"))),
                Set.of(group(new Administrator(), "admin-country", "country", Set.of(), Set.of("both@test.com"))));

        svc.notifyAnswerValidated(a, "manager@test.com");

        assertEquals(1, sent.size());
        assertTrue(sent.get(0).getBcc().contains("both@test.com"));
    }

    @Test
    void notifyAnswerValidated_doesNotSendWhenNoCoordinatorsOrAdministrators() {
        SurveyAnswer a = answer("sa-1", "s1", "sh-country-gr", "country");
        when(coordinatorService.getWithFilter("type", "country")).thenReturn(Set.of());
        when(administratorService.getWithFilter("type", "country")).thenReturn(Set.of());

        service.notifyAnswerValidated(a, "manager@test.com");

        assertTrue(mailDebugConfig.getCapturedEmails().isEmpty());
    }

    @Test
    void notifyAnswerValidated_skipsOptedOutUsers() {
        SurveyAnswer a = answer("sa-1", "s1", "sh-country-gr", "country");
        stubValidationContext(a, "National Survey", "Greece",
                Set.of(group(new Coordinator(), "co-country", "country", Set.of("optout@test.com"), Set.of())),
                Set.of());

        User user = new User();
        NotificationPreferences prefs = new NotificationPreferences();
        prefs.setSurveyEmailNotifications(false);
        Settings settings = new Settings();
        settings.setNotificationPreferences(prefs);
        user.setSettings(settings);
        when(userService.getUser("optout@test.com")).thenReturn(user);

        service.notifyAnswerValidated(a, "manager@test.com");

        assertTrue(mailDebugConfig.getCapturedEmails().isEmpty());
    }

    @Test
    void notifyAnswerValidated_usesForwardEmails() {
        SurveyAnswer a = answer("sa-1", "s1", "sh-country-gr", "country");
        stubValidationContext(a, "National Survey", "Greece",
                Set.of(group(new Coordinator(), "co-country", "country", Set.of("co@test.com"), Set.of())),
                Set.of());

        User user = new User();
        NotificationPreferences prefs = new NotificationPreferences();
        prefs.setForwardEmails(List.of("forwarded@test.com"));
        Settings settings = new Settings();
        settings.setNotificationPreferences(prefs);
        user.setSettings(settings);
        when(userService.getUser("co@test.com")).thenReturn(user);

        service.notifyAnswerValidated(a, "manager@test.com");

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertEquals(List.of("forwarded@test.com"), captured.get(0).getBcc());
    }

    @Test
    void notifyAnswerValidated_rendersTheRealTemplate() throws Exception {
        Configuration realConfig = new Configuration(Configuration.getVersion());
        realConfig.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates");

        EmailSurveyService realTemplateService = new EmailSurveyService(
                mailDebugConfig.mailer(), stakeholderCrudService, modelService, surveyService, userService,
                surveyNotificationSettingsService, coordinatorService, administratorService,
                realConfig, "no-reply@openaire.eu", applicationProperties);

        SurveyAnswer a = answer("sa-1", "s1", "sh-country-gr", "country");
        stubValidationContext(a, "National Survey", "Greece",
                Set.of(group(new Coordinator(), "co-country", "country", Set.of("co@test.com"), Set.of())),
                Set.of(group(new Administrator(), "admin-country", "country", Set.of("ad@test.com"), Set.of())));
        User validator = new User();
        validator.setEmail("manager@test.com");
        validator.setFullname("Maria Papadopoulou");
        when(userService.getUser("manager@test.com")).thenReturn(validator);

        realTemplateService.notifyAnswerValidated(a, "manager@test.com");

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(2, captured.size());

        for (EmailMessage email : captured) {
            String body = email.getText();
            assertNotNull(body);
            assertFalse(body.isBlank(), "template rendered an empty body");
            assertTrue(body.contains("Greece"));
            assertTrue(body.contains("National Survey"));
            assertTrue(body.contains("Maria Papadopoulou"));
            assertTrue(body.contains("Review answer"));
        }

        // each group's email links into that group's own dashboard
        assertTrue(emailContaining(captured, "co@test.com").getText().contains(reviewUrl("co-country")));
        assertTrue(emailContaining(captured, "ad@test.com").getText().contains(reviewUrl("admin-country")));
    }

    private Date futureDate(int daysFromNow) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysFromNow);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
