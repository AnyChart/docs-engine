CREATE SEQUENCE page_id_seq;
CREATE TABLE pages (
  id            INTEGER PRIMARY KEY DEFAULT nextval('page_id_seq'),
  version_id    INTEGER REFERENCES versions (id),
  url           VARCHAR(255) NOT NULL,
  full_name     VARCHAR(255),
  content       TEXT,
  last_modified BIGINT,
  tags          VARCHAR(255) [],
  config        JSONB
);


CREATE SEQUENCE folder_id_seq;
CREATE TABLE folders (
  id           INTEGER PRIMARY KEY DEFAULT nextval('folder_id_seq'),
  version_id   INTEGER REFERENCES versions (id),
  url          VARCHAR(255),
  default_page VARCHAR(255)
);


CREATE SEQUENCE version_id_seq;
CREATE TABLE versions (
  id     INTEGER PRIMARY KEY DEFAULT nextval('version_id_seq'),
  key    VARCHAR(255) NOT NULL,
  commit VARCHAR(40)  NOT NULL,
  hidden BOOLEAN             DEFAULT FALSE,
  tree   TEXT,
  zip    BYTEA,
  config JSONB,
  report JSONB
);
