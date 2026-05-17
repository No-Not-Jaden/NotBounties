CREATE TABLE IF NOT EXISTS player (
    uuid BINARY(16) NOT NULL,
    name VARCHAR(16) NOT NULL UNIQUE,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    immunity_types TINYINT NOT NULL DEFAULT 0,
    broadcast_setting TINYINT NOT NULL DEFAULT 0,
    last_claim BIGINT NOT NULL DEFAULT 0,
    b_cooldown BIGINT NOT NULL DEFAULT 0,
    playtime BIGINT NOT NULL DEFAULT 0,
    last_seen BIGINT NOT NULL DEFAULT 0,
    time_zone VARCHAR(32),
    texture_id CHAR(64),
    whitelist_mode BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (uuid)
);

CREATE TABLE IF NOT EXISTS stat (
    uuid BINARY(16) NOT NULL,
    b_claimed INT NOT NULL DEFAULT 0,
    b_set INT NOT NULL DEFAULT 0,
    b_received INT NOT NULL DEFAULT 0,
    b_all_time DOUBLE NOT NULL DEFAULT 0,
    immunity DOUBLE NOT NULL DEFAULT 0,
    b_claim_amt DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (uuid),
    FOREIGN KEY (uuid) REFERENCES player(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tag (
    bounty_id INT NOT NULL,
    tag_value VARCHAR(32) NOT NULL,
    PRIMARY KEY (bounty_id, tag_value),
    FOREIGN KEY (bounty_id) REFERENCES bounty(bounty_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS item (
    item_id INT NOT NULL AUTO_INCREMENT,
    item_list BLOB,
    PRIMARY KEY (item_id)
);

CREATE TABLE IF NOT EXISTS refund (
    refund_id INT NOT NULL AUTO_INCREMENT,
    uuid BINARY(16),
    time BIGINT,
    item_id INT,
    refund_amount DOUBLE,
    reason VARCHAR(256),
    PRIMARY KEY (refund_id),
    FOREIGN KEY (uuid) REFERENCES player(uuid) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bounty (
    bounty_id INT NOT NULL AUTO_INCREMENT,
    setter BINARY(16) NOT NULL,
    receiver BINARY(16) NOT NULL,
    item_id INT,
    amount DOUBLE NOT NULL,
    display DOUBLE NOT NULL,
    notified BOOLEAN NOT NULL DEFAULT FALSE,
    time_placed BIGINT NOT NULL,
    playtime BIGINT NOT NULL,
    whitelist_mode BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (bounty_id),
    FOREIGN KEY (setter) REFERENCES player(uuid) ON DELETE NO ACTION,
    FOREIGN KEY (receiver) REFERENCES player(uuid) ON DELETE NO ACTION,
    FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS whitelist (
    owner BINARY(16),
    uuid BINARY(16),
    PRIMARY KEY (owner, player),
    FOREIGN KEY (owner) REFERENCES player(uuid) ON DELETE CASCADE,
    FOREIGN KEY (uuid) REFERENCES player(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bounty_whitelist (
    bounty_id INT,
    uuid BINARY(16),
    PRIMARY KEY (bounty_id, player),
    FOREIGN KEY (bounty_id) REFERENCES bounty(bounty_id) ON DELETE CASCADE,
    FOREIGN KEY (uuid) REFERENCES player(uuid) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS message (
    message_id INT NOT NULL,
    server_id BINARY(16) NOT NULL,
    contents VARCHAR(256) NOT NULL,
    destination BINARY(16) NOT NULL,
    PRIMARY KEY (message_id)
);
-- TODO: ADD INDEXES (pages)