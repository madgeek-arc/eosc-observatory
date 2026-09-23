# Coordinated user erasure

This procedure assumes exactly one active backend, including during deployments.

1. Ask the user to finish editing, log out on every browser/device, and close all
   application tabs. They must not log in until the operator confirms completion.
2. Pause administrative edits to stakeholders, coordinators, and administrators.
   Discard open membership forms and reload them after erasure; a stale form can
   reintroduce the removed email even after the purge has finished.
3. Run the user purge. In-flight group HTTP writes finish before cleanup starts;
   new group writes during cleanup receive HTTP 409. Background/direct database
   administration must also remain paused. Pending survey drafts may remain:
   cleanup preserves their changes and removes identity references.
4. Confirm that the purge returned successfully, the user resource is absent, and
   the erasure register has SUCCESS for this attempt. PENDING is not completion.
   On failure, keep the pause in place and retry the purge. A retry can finish the
   register update when deletion already succeeded.
5. Tell the user erasure is complete, then resume administrative edits using fresh
   data. The user may log in again as a returning user.

The application does not enforce the user's logout/pause. Session revocation,
forced WebSocket closure, and distributed locking remain deferred. Live-edit
writes over WebSocket are now re-authorized per message against current
permissions, so a lingering session can no longer write after its permission
is revoked or the account is erased; the session itself is not forcibly closed.

## Erasure register

Each attempt has its own `attempt_id`. `subject_ref`, `started_at`, the operator,
and cleanup counts are immutable once stored. Only `outcome` and `completed_at`
change on completion. Retrying resumes the pending attempt and retains its audit
fields; a later erasure of a returning user creates another row.

The pending snapshot is recorded after cleanup and before user deletion. Its
counts describe changes observed in that cleanup pass, not a verification that
no references remain. Failures before this snapshot are not recorded in this
register. `completed_at` stays null until deletion succeeds. Check the current
attempt, not merely the existence of an older SUCCESS for the same subject.

Starting an attempt acquires a PostgreSQL transaction advisory lock for the
subject before looking for a pending row. Competing starts wait for commit and
reuse the same attempt. This protects register creation across backends; it does
not make the rest of the purge safe for multi-instance operation.
