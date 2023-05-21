--liquibase formatted sql

--changeset SE-Grebenyuk:01.000.00/TVR-1/init-schema
--rollback drop index if exists idx_news_published_at_desc;
--rollback drop table if exists tradevisor.news;
create table if not exists tradevisor.news
(
    hash_id varchar not null primary key,
    source  varchar,
    title varchar,
    description varchar,
    url varchar,
    published_at timestamp with time zone
);
CREATE INDEX if not exists idx_news_published_at_desc  ON tradevisor.news (published_at DESC);
