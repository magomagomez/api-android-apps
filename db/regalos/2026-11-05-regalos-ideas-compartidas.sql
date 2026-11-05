-- regalos-api v0.6: an idea can be shared with people of the app, who may then give it.
-- Never with its author or with whom it is for: regalos-api refuses both (R13).
-- Idempotent: safe to run twice.

CREATE TABLE IF NOT EXISTS regalos_idea_shares (
    idea_id BIGINT NOT NULL REFERENCES regalos_ideas(id) ON DELETE CASCADE,
    user_id INT    NOT NULL REFERENCES regalos_users(id) ON DELETE CASCADE,
    PRIMARY KEY (idea_id, user_id)
);
CREATE INDEX IF NOT EXISTS regalos_idea_shares_user_idx ON regalos_idea_shares(user_id);

-- At most one giver per shared idea (R14): the primary key lets only one claim in.
CREATE TABLE IF NOT EXISTS regalos_idea_claims (
    idea_id    BIGINT      PRIMARY KEY REFERENCES regalos_ideas(id) ON DELETE CASCADE,
    user_id    INT         NOT NULL REFERENCES regalos_users(id) ON DELETE CASCADE,
    state      VARCHAR(10) NOT NULL CHECK (state IN ('RESERVED', 'BOUGHT')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
