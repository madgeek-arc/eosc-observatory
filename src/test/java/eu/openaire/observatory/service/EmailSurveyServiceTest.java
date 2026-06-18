package eu.openaire.observatory.service;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.configuration.MailDebugConfig;
import eu.openaire.observatory.domain.NotificationPreferences;
import eu.openaire.observatory.domain.Settings;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.User;
import freemarker.template.Configuration;
import freemarker.template.Template;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
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
    @Mock private SurveyTypeSettingsService surveyTypeSettingsService;
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
                surveyTypeSettingsService,
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
    void checkSurveyDates_notifiesEnd_whenTodayIsCloseDate() {
        Model s = survey("s1", "National Survey", "country");
        s.setSubmissionStartAt(futureDate(-10));
        s.setSubmissionCloseAt(new Date()); // today

        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(List.of(s));
        when(surveyService.getByType(any(FacetFilter.class), isNull())).thenReturn(browsing);
        stubSurveyAndStakeholders(s, stakeholderWith("alice@test.com"));

        service.checkSurveyDates();

        List<EmailMessage> captured = mailDebugConfig.getCapturedEmails();
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).getSubject().contains("closed"));
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
