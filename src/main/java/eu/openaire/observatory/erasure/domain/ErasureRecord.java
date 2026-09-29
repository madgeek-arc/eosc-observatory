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

package eu.openaire.observatory.erasure.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Durable record of an erasure attempt. Completed attempts are never overwritten.
 *
 * <p>Art. 17 gives the erasure right, but Art. 5(2) and Art. 24 require demonstrating compliance
 * afterwards, potentially after the log line {@code UserServiceImpl#purge} writes has aged out of
 * retention. This table is that evidence.
 *
 * <p>The subject is represented by pseudonymous data: {@link #subjectRef} is the keyed HMAC produced
 * by {@code ErasureSubjectReference}, alongside attempt identifiers, timestamps, and cleanup counts.
 * {@link #executedBy} is the administrator who ran the erasure, retained as accountability data.
 *
 * <p>Wired by {@code ErasureDatasourceConfig} onto its own {@code erasure} datasource and schema on
 * the registry database, created by {@code hbm2ddl.auto=update}.
 *
 * <p>Field access (the {@code @Id} annotation is on a field), so the fluent setters below are
 * caller convenience only — Hibernate never invokes them.
 */
@Entity
@Table(name = "erasure_record", schema = "erasure",
        indexes = @Index(name = "erasure_record_subject_idx", columnList = "subject_ref"),
        uniqueConstraints = @UniqueConstraint(name = "erasure_record_one_pending_subject",
                columnNames = "pending_subject_ref"))
public class ErasureRecord {

    @Id
    @Column(name = "attempt_id", nullable = false, updatable = false)
    private UUID attemptId = UUID.randomUUID();

    /** Multiple completed attempts may belong to the same pseudonymous subject. */
    @Column(name = "subject_ref", nullable = false, updatable = false)
    private String subjectRef;

    /** Database-derived: only pending attempts occupy the subject's unique slot. */
    @Column(name = "pending_subject_ref", insertable = false, updatable = false,
            columnDefinition = "varchar(255) GENERATED ALWAYS AS "
                    + "(CASE WHEN outcome = 'PENDING' THEN subject_ref ELSE NULL END) STORED")
    private String pendingSubjectRef;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    /** Null until deletion and the completion transaction succeed. */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** Id of the administrator who performed the erasure; null if it ran outside a security context. */
    @Column(name = "executed_by", updatable = false)
    private String executedBy;

    /**
     * Id of whoever requested the erasure, when a request workflow records it separately. Nullable
     * today — there is no data-subject-request intake feeding this.
     */
    @Column(name = "requested_by", updatable = false)
    private String requestedBy;

    @Column(name = "stakeholder_groups", nullable = false, updatable = false)
    private int stakeholderGroups;

    @Column(name = "coordinator_groups", nullable = false, updatable = false)
    private int coordinatorGroups;

    @Column(name = "administrator_groups", nullable = false, updatable = false)
    private int administratorGroups;

    @Column(name = "survey_answers", nullable = false, updatable = false)
    private int surveyAnswers;

    @Column(name = "news_items", nullable = false, updatable = false)
    private int newsItems;

    @Column(name = "survey_definitions", nullable = false, updatable = false)
    private int surveyDefinitions;

    @Column(name = "documents", nullable = false, updatable = false)
    private int documents;

    @Column(name = "messaging_threads", nullable = false, updatable = false)
    private int messagingThreads;

    /** PENDING until user deletion and the completion transaction both succeed; then SUCCESS. */
    @Column(name = "outcome", nullable = false)
    private String outcome;

    public ErasureRecord() {
        // no-arg constructor for JPA
    }

    public UUID getAttemptId() {
        return attemptId;
    }

    public String getSubjectRef() {
        return subjectRef;
    }

    public ErasureRecord setSubjectRef(String subjectRef) {
        this.subjectRef = subjectRef;
        return this;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public ErasureRecord setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public ErasureRecord setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public ErasureRecord setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
        return this;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public ErasureRecord setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
        return this;
    }

    public int getStakeholderGroups() {
        return stakeholderGroups;
    }

    public ErasureRecord setStakeholderGroups(int stakeholderGroups) {
        this.stakeholderGroups = stakeholderGroups;
        return this;
    }

    public int getCoordinatorGroups() {
        return coordinatorGroups;
    }

    public ErasureRecord setCoordinatorGroups(int coordinatorGroups) {
        this.coordinatorGroups = coordinatorGroups;
        return this;
    }

    public int getAdministratorGroups() {
        return administratorGroups;
    }

    public ErasureRecord setAdministratorGroups(int administratorGroups) {
        this.administratorGroups = administratorGroups;
        return this;
    }

    public int getSurveyAnswers() {
        return surveyAnswers;
    }

    public ErasureRecord setSurveyAnswers(int surveyAnswers) {
        this.surveyAnswers = surveyAnswers;
        return this;
    }

    public int getNewsItems() {
        return newsItems;
    }

    public ErasureRecord setNewsItems(int newsItems) {
        this.newsItems = newsItems;
        return this;
    }

    public int getSurveyDefinitions() {
        return surveyDefinitions;
    }

    public ErasureRecord setSurveyDefinitions(int surveyDefinitions) {
        this.surveyDefinitions = surveyDefinitions;
        return this;
    }

    public int getDocuments() {
        return documents;
    }

    public ErasureRecord setDocuments(int documents) {
        this.documents = documents;
        return this;
    }

    public int getMessagingThreads() {
        return messagingThreads;
    }

    public ErasureRecord setMessagingThreads(int messagingThreads) {
        this.messagingThreads = messagingThreads;
        return this;
    }

    public String getOutcome() {
        return outcome;
    }

    public ErasureRecord setOutcome(String outcome) {
        this.outcome = outcome;
        return this;
    }
}
