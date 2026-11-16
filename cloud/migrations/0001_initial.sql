CREATE TABLE IF NOT EXISTS users (
 username TEXT PRIMARY KEY,
 role TEXT NOT NULL CHECK(role IN ('admin','member')),
 pass_salt TEXT NOT NULL,
 pass_hash TEXT NOT NULL,
 active INTEGER NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS sessions (
 token_hash TEXT PRIMARY KEY,
 username TEXT NOT NULL REFERENCES users(username),
 expires_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sessions_expiry ON sessions(expires_at);
CREATE TABLE IF NOT EXISTS records (
 id TEXT NOT NULL,
 kind TEXT NOT NULL CHECK(kind IN ('tasks','packs','alarms')),
 owner TEXT NOT NULL,
 updated_at INTEGER NOT NULL,
 payload TEXT NOT NULL,
 PRIMARY KEY(kind,id)
);
CREATE INDEX IF NOT EXISTS idx_record_owner ON records(owner,kind);
