CREATE DATABASE IF NOT EXISTS legend;
USE legend;

CREATE TABLE IF NOT EXISTS legend.player_stats
(
    uuid  VARCHAR(36) PRIMARY KEY NOT NULL,
    coins BIGINT                  NOT NULL
);

CREATE TABLE IF NOT EXISTS legend.quest_model
(
    name         VARCHAR(40) PRIMARY KEY NOT NULL,
    display_name VARCHAR(255)            NOT NULL,
    permission   VARCHAR(50)             NOT NULL,
    description  TEXT,
    duration     BIGINT
);

CREATE TABLE IF NOT EXISTS legend.quest_reward
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    quest_reward_type VARCHAR(50)                       NOT NULL,
    reward            TEXT
);

CREATE TABLE IF NOT EXISTS legend.quest_requirement
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    quest_requirement_type VARCHAR(50)                       NOT NULL,
    required_amount        INT,
    quest_name             VARCHAR(40)                       NOT NULL,
    entity_type            VARCHAR(90),
    material               VARCHAR(90),
    FOREIGN KEY (quest_name) REFERENCES quest_model (name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS legend.quest_reward_relation
(
    quest_name VARCHAR(40) NOT NULL,
    reward_id  BIGINT      NOT NULL,
    PRIMARY KEY (quest_name, reward_id),
    FOREIGN KEY (quest_name) REFERENCES legend.quest_model (name) ON DELETE CASCADE,
    FOREIGN KEY (reward_id) REFERENCES legend.quest_reward (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quest_user_progress
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    quest_requirement_type VARCHAR(50)                       NOT NULL,
    quest_name             VARCHAR(40)                       NOT NULL,
    uuid                   VARCHAR(36)                       NOT NULL,
    requirement_id         BIGINT                            NOT NULL,
    progress               INT                               NOT NULL,
    expiration             DATETIME                          NOT NULL,
    FOREIGN KEY (quest_name) REFERENCES quest_model (name) ON DELETE CASCADE,
    FOREIGN KEY (uuid) REFERENCES player_stats (uuid) ON DELETE CASCADE,
    FOREIGN KEY (requirement_id) REFERENCES quest_requirement (id) ON DELETE CASCADE
);