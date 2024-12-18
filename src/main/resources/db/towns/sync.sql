CREATE TABLE IF NOT EXISTS "town" (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(128) UNIQUE NOT NULL,
    founder VARCHAR(36) NOT NULL,
    mayor VARCHAR(36) UNIQUE,
    money INTEGER NOT NULL DEFAULT 0,
    plots INTEGER NOT NULL DEFAULT 0,
    color VARCHAR(12) NOT NULL DEFAULT '#55FFFF'
);

CREATE TABLE IF NOT EXISTS "town_member" (
    player_id VARCHAR(36) UNIQUE NOT NULL,
    town_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (player_id, town_id),
    FOREIGN KEY (town_id) REFERENCES town(id)
);

CREATE TABLE IF NOT EXISTS "town_plot" (
    world_id VARCHAR(36) NOT NULL,
    chunk_x INTEGER NOT NULL,
    chunk_z INTEGER NOT NULL,
    town_id VARCHAR(36),
    PRIMARY KEY (world_id, chunk_x, chunk_z),
    FOREIGN KEY (town_id) REFERENCES town(id)
);

CREATE TABLE IF NOT EXISTS "town_message" (
    id INTEGER PRIMARY KEY,
    town_id VARCHAR(36) NOT NULL,
    message TEXT NOT NULL,
    created_at INTEGER DEFAULT (UNIXEPOCH()),
    FOREIGN KEY (town_id) REFERENCES town(id)
);