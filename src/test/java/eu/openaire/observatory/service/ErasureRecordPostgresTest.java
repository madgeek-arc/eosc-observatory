package eu.openaire.observatory.service;

import eu.openaire.observatory.erasure.domain.ErasureRecord;
import eu.openaire.observatory.erasure.repository.ErasureRecordRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in PostgreSQL test: uses only a newly created, randomly named schema. */
@EnabledIfSystemProperty(named = "registry.datasource.url", matches = "jdbc:postgresql:.*")
class ErasureRecordPostgresTest {
    @Test
    void attemptLifecyclePreservesAuditEvidence() throws Exception {
        String url = System.getProperty("registry.datasource.url");
        String username = System.getProperty("registry.datasource.username");
        String password = System.getProperty("registry.datasource.password");
        String schema = "erasure_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(url, username, password);
             var sql = connection.createStatement()) {
            sql.execute("CREATE SCHEMA " + schema);
            try {
                var registry = new StandardServiceRegistryBuilder()
                        .applySetting("hibernate.connection.url", url)
                        .applySetting("hibernate.connection.username", username)
                        .applySetting("hibernate.connection.password", password)
                        .applySetting("hibernate.hbm2ddl.auto", "create")
                        .applySetting("hibernate.physical_naming_strategy", new PhysicalNamingStrategyStandardImpl() {
                            @Override
                            public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment environment) {
                                return Identifier.toIdentifier(schema);
                            }
                        }).build();
                try (var factory = new MetadataSources(registry).addAnnotatedClass(ErasureRecord.class)
                        .buildMetadata().buildSessionFactory()) {
                    verifyAttempts(factory);
                    verifyConcurrentStarts(factory);
                    verifyConstraintRejectsDuplicatePending(factory);
                } finally {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
            } finally {
                sql.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private void verifyAttempts(SessionFactory factory) {
        Instant started = Instant.parse("2026-01-01T00:00:00Z");
        UUID first = transact(factory, session -> new ErasureRegisterService(repository(session)).begin(
                new ErasureRecord().setSubjectRef("returning-user").setStartedAt(started)
                        .setExecutedBy("first-operator").setSurveyAnswers(4)));
        UUID retry = transact(factory, session -> new ErasureRegisterService(repository(session)).begin(
                new ErasureRecord().setSubjectRef("returning-user").setStartedAt(Instant.now())
                        .setExecutedBy("retry-operator").setSurveyAnswers(0)));
        assertEquals(first, retry);

        // A failure committing completion leaves the durable pending attempt available for retry.
        try (var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            new ErasureRegisterService(repository(session)).complete(first);
            session.flush();
            transaction.rollback();
        }
        transact(factory, session -> {
            assertTrue(new ErasureRegisterService(repository(session)).isPending("returning-user"));
            // Even accidental setter calls cannot rewrite immutable audit columns through Hibernate.
            var record = session.find(ErasureRecord.class, first);
            record.setSubjectRef("changed").setStartedAt(Instant.now())
                    .setExecutedBy("changed").setSurveyAnswers(99);
            new ErasureRegisterService(repository(session)).complete(first);
            return null;
        });
        Instant completed = transact(factory, session -> {
            var record = session.find(ErasureRecord.class, first);
            assertEquals("returning-user", record.getSubjectRef());
            assertEquals(started, record.getStartedAt());
            assertEquals("first-operator", record.getExecutedBy());
            assertEquals(4, record.getSurveyAnswers());
            assertEquals("SUCCESS", record.getOutcome());
            assertNotNull(record.getCompletedAt());
            return record.getCompletedAt();
        });
        UUID second = transact(factory, session -> new ErasureRegisterService(repository(session)).begin(
                new ErasureRecord().setSubjectRef("returning-user").setStartedAt(Instant.now())
                        .setExecutedBy("second-operator").setSurveyAnswers(2)));
        assertNotEquals(first, second);
        transact(factory, session -> {
            var repository = repository(session);
            var service = new ErasureRegisterService(repository);
            service.complete(first); // Idempotent completion must not complete the newer attempt.
            assertEquals(completed, repository.findById(first).orElseThrow().getCompletedAt());
            assertEquals("PENDING", repository.findById(second).orElseThrow().getOutcome());
            service.complete(second);
            assertEquals(2, repository.findAllBySubjectRefOrderByStartedAtAsc("returning-user").size());
            return null;
        });
    }

    private void verifyConcurrentStarts(SessionFactory factory) throws Exception {
        var secondStarted = new CountDownLatch(1);
        try (var firstSession = factory.openSession(); var executor = Executors.newSingleThreadExecutor()) {
            var transaction = firstSession.beginTransaction();
            try {
                UUID first = new ErasureRegisterService(repository(firstSession)).begin(
                        new ErasureRecord().setSubjectRef("concurrent-user").setStartedAt(Instant.now())
                                .setSurveyAnswers(7));
                firstSession.flush();
                var second = executor.submit(() -> transact(factory, session -> {
                    secondStarted.countDown();
                    return new ErasureRegisterService(repository(session)).begin(
                            new ErasureRecord().setSubjectRef("concurrent-user").setStartedAt(Instant.now())
                                    .setSurveyAnswers(0));
                }));
                assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
                transaction.commit();
                assertEquals(first, second.get(5, TimeUnit.SECONDS));
                transact(factory, session -> {
                    var attempts = repository(session).findAllBySubjectRefOrderByStartedAtAsc("concurrent-user");
                    assertEquals(1, attempts.size());
                    assertEquals(7, attempts.getFirst().getSurveyAnswers());
                    return null;
                });
            } finally {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
            }
        }
    }

    private void verifyConstraintRejectsDuplicatePending(SessionFactory factory) {
        var error = assertThrows(org.hibernate.exception.ConstraintViolationException.class,
                () -> transact(factory, session -> {
                    // Bypass service locking: the database itself must enforce uniqueness.
                    session.persist(new ErasureRecord().setSubjectRef("concurrent-user")
                            .setStartedAt(Instant.now()).setOutcome("PENDING"));
                    session.flush();
                    return null;
                }));
        assertEquals("23505", error.getSQLState());
        assertEquals("erasure_record_one_pending_subject", error.getConstraintName());
    }

    private static ErasureRecordRepository repository(Session session) {
        return new JpaRepositoryFactory(session).getRepository(ErasureRecordRepository.class);
    }

    private static <T> T transact(SessionFactory factory, Function<Session, T> action) {
        try (var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            try {
                T result = action.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException | Error failure) {
                transaction.rollback();
                throw failure;
            }
        }
    }
}
