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

package eu.openaire.observatory.commenting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Durable, append-only record that a user erasure ran — one row per erased data subject.
 *
 * <p>Art. 17 gives the erasure right, but Art. 5(2) and Art. 24 require the controller to be able to
 * <em>demonstrate</em> compliance afterwards, potentially long after the log line that
 * {@code UserServiceImpl#purge} writes has aged out of the log-retention window. This table is that
 * evidence.
 *
 * <p>It holds no personal data of the erased subject: {@link #subjectRef} is the keyed HMAC produced
 * by {@code ErasureSubjectReference} (pseudonymous, not reversible from the row) and every other
 * column is a count or an outcome flag. {@link #executedBy} is the id of the administrator who ran
 * the erasure, retained as controller-accountability data.
 *
 * <p>Rides the {@code commenting} datasource purely to reuse an existing app-owned JPA store on the
 * registry database; it is otherwise unrelated to commenting. Picked up with no extra configuration
 * by the {@code @EntityScan} / {@code @EnableJpaRepositories} in {@code CommentingDatasourceConfig};
 * the table is created by that datasource's {@code hbm2ddl.auto=update}.
 *
 * <p>Field access (the {@code @Id} annotation is on a field), so the fluent setters below are
 * caller convenience only — Hibernate never invokes them.
 */
@Entity
@Table(name = "erasure_record", schema = "commenting")
public class ErasureRecord {

    /**
     * Keyed HMAC of the erased user id (see {@code ErasureSubjectReference}). Natural primary key:
     * the same subject always maps to the same row, so re-running {@code purge()} after a partial
     * failure cannot create a duplicate and the first record stays authoritative.
     */
    @Id
    @Column(name = "subject_ref", nullable = false, updatable = false)
    private String subjectRef;

    @Column(name = "erased_at", nullable = false, updatable = false)
    private Instant erasedAt;

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

    /** {@code "SUCCESS"} — the row is written only on the success path, just before {@code delete(id)}. */
    @Column(name = "outcome", nullable = false, updatable = false)
    private String outcome;

    public ErasureRecord() {
        // no-arg constructor for JPA
    }

    public String getSubjectRef() {
        return subjectRef;
    }

    public ErasureRecord setSubjectRef(String subjectRef) {
        this.subjectRef = subjectRef;
        return this;
    }

    public Instant getErasedAt() {
        return erasedAt;
    }

    public ErasureRecord setErasedAt(Instant erasedAt) {
        this.erasedAt = erasedAt;
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
