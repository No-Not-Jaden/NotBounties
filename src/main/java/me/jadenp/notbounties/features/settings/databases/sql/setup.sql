CREATE TABLE player (
    uuid CHAR(36),
    name VARCHAR(16),
    online BOOLEAN,
    immunity_types TINYINT,
    broadcast_setting TINYINT,
    last_claim BIGING,
    b_cooldown BIGINT,
    new_player BOOLEAN,
    last_seen BIGINT,
    time_zone VARCHAR(32),
    PRIMARY KEY (uuid)
);

CREATE TABLE stat (
    stat_id INT,
    uuid CHAR(36),
    b_claimed INT,
    b_set INT,
    b_received INT,
    b_all_time FLOAT(53),
    immunity FLOAT(53),
    b_claim_amt FLOAT(53),
    PRIMARY KEY (stat_id),
    FOREIGN KEY (uuid) REFERENCES player(uuid) ON DELETE CASCADE
);

CREATE TABLE tag (
    tag_id INT,
    tag_value VARCHAR(32),
    PRIMARY KEY (tag_id)
);

CREATE TABLE item (
    item_id INT,
    item_list BLOB,
    PRIMARY KEY (item_id)
);

CREATE TABLE refund (
    refund_id INT,
    uuid CHAR(36),
    time BIGINT,
    item_id INT,
    refund_amount FLOAT(53),
    reason VARCHAR(256),
    PRIMARY KEY (refund_id),
    FOREIGN KEY (uuid) REFERENCES player(uuid) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE CASCADE
);