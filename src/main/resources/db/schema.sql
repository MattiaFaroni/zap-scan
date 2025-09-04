CREATE TABLE reports
(
    id           SERIAL PRIMARY KEY,
    endpoint_id  INT       NOT NULL REFERENCES endpoints (id) ON DELETE CASCADE,
    executed_at  TIMESTAMP NOT NULL,
    filename     TEXT,
    content_type VARCHAR(100),
    report       BYTEA
);

CREATE TABLE endpoints
(
    id           SERIAL PRIMARY KEY,
    name         TEXT        NOT NULL,
    url          TEXT        NOT NULL,
    http_method  VARCHAR(10) NOT NULL,
    query_params JSONB,
    headers      JSONB,
    request_body JSONB
);
