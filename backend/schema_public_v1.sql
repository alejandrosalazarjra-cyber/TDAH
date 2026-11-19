-- NeuroSync API pública, solo base independiente; PostgreSQL 14+.
BEGIN;

CREATE TABLE IF NOT EXISTS ns_public_users (
  id uuid PRIMARY KEY,
  provider_subject text NOT NULL UNIQUE,
  handle text NOT NULL UNIQUE CHECK (handle ~ '^[a-z0-9_]{3,30}$'),
  display_name text NOT NULL CHECK (length(display_name) BETWEEN 1 AND 100),
  discoverable boolean NOT NULL DEFAULT false,
  disabled_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ns_public_sessions (
  token_hash text PRIMARY KEY CHECK (token_hash ~ '^[a-f0-9]{64}$'),
  user_id uuid NOT NULL REFERENCES ns_public_users(id) ON DELETE CASCADE,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ns_sessions_active_user ON ns_public_sessions(user_id,expires_at)
  WHERE revoked_at IS NULL;

CREATE TABLE IF NOT EXISTS ns_contact_invitations (
  id uuid PRIMARY KEY,
  requester_id uuid NOT NULL REFERENCES ns_public_users(id),
  recipient_id uuid NOT NULL REFERENCES ns_public_users(id),
  state text NOT NULL DEFAULT 'pending'
    CHECK (state IN ('pending','accepted','declined','cancelled')),
  created_at timestamptz NOT NULL DEFAULT now(),
  changed_at timestamptz NOT NULL DEFAULT now(),
  CHECK (requester_id <> recipient_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS ns_pending_invite_pair
  ON ns_contact_invitations (requester_id,recipient_id) WHERE state='pending';

CREATE TABLE IF NOT EXISTS ns_contact_blocks (
  blocker_id uuid NOT NULL REFERENCES ns_public_users(id),
  blocked_id uuid NOT NULL REFERENCES ns_public_users(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (blocker_id,blocked_id),
  CHECK (blocker_id <> blocked_id)
);

CREATE TABLE IF NOT EXISTS ns_public_tasks (
  id uuid PRIMARY KEY,
  sender_id uuid NOT NULL REFERENCES ns_public_users(id),
  recipient_id uuid NOT NULL REFERENCES ns_public_users(id),
  title text NOT NULL CHECK (length(trim(title)) BETWEEN 1 AND 100),
  body jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(body)='object'),
  status text NOT NULL DEFAULT 'pending'
    CHECK (status IN ('pending','accepted','declined','completed')),
  version bigint NOT NULL DEFAULT 1 CHECK (version >= 1),
  created_at timestamptz NOT NULL DEFAULT now(),
  accepted_at timestamptz,
  completed_at timestamptz,
  updated_at timestamptz NOT NULL DEFAULT now(),
  CHECK (sender_id <> recipient_id)
);
CREATE INDEX IF NOT EXISTS ns_tasks_received ON ns_public_tasks(recipient_id,updated_at DESC,id);
CREATE INDEX IF NOT EXISTS ns_tasks_sent ON ns_public_tasks(sender_id,updated_at DESC,id);

CREATE TABLE IF NOT EXISTS ns_task_idempotency (
  sender_id uuid NOT NULL REFERENCES ns_public_users(id),
  request_key text NOT NULL CHECK (length(request_key) BETWEEN 8 AND 100),
  payload_sha256 text NOT NULL CHECK (payload_sha256 ~ '^[a-f0-9]{64}$'),
  task_id uuid NOT NULL UNIQUE REFERENCES ns_public_tasks(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (sender_id,request_key)
);

CREATE TABLE IF NOT EXISTS ns_user_events (
  sequence bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES ns_public_users(id),
  task_id uuid NOT NULL REFERENCES ns_public_tasks(id),
  event_type text NOT NULL CHECK
    (event_type IN ('task_received','task_accepted','task_declined','task_completed')),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ns_events_for_user ON ns_user_events(user_id,sequence);

CREATE TABLE IF NOT EXISTS ns_device_push_tokens (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES ns_public_users(id),
  token_hash text NOT NULL UNIQUE CHECK (length(token_hash)=64),
  token_ciphertext bytea NOT NULL,
  platform text NOT NULL CHECK (platform IN ('android')),
  revoked_at timestamptz,
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ns_push_active_user
  ON ns_device_push_tokens(user_id) WHERE revoked_at IS NULL;
COMMIT;
