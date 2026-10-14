-- Catalog: taxonomy (Platform aggregate)
create table platform (
    id           varchar(36)  primary key,
    name         varchar(120) not null,
    kind         varchar(20)  not null,
    lock_version bigint       not null default 0
);

create table category (
    id          varchar(36)  primary key,
    platform_id varchar(36)  not null references platform (id) on delete cascade,
    name        varchar(120) not null,
    position    int
);
create index ix_category_platform on category (platform_id);

create table subcategory (
    id          varchar(36)  primary key,
    category_id varchar(36)  not null references category (id) on delete cascade,
    name        varchar(120) not null,
    position    int
);
create index ix_subcategory_category on subcategory (category_id);

-- Catalog: keys (Key aggregate)
create table catalog_key (
    id               varchar(36)  primary key,
    subcategory_id   varchar(36)  not null references subcategory (id),
    term             varchar(160) not null,
    search_text      varchar(2000) not null,
    aliases          text         not null,
    short_definition varchar(280) not null,
    detail           text         not null,
    java_example     text         not null,
    difficulty       varchar(10)  not null,
    status           varchar(12)  not null,
    tags             text         not null,
    related_terms    text         not null,
    created_at       timestamp with time zone not null,
    updated_at       timestamp with time zone not null,
    lock_version     bigint       not null default 0,
    constraint ux_key_term unique (subcategory_id, term)
);
create index ix_key_status on catalog_key (status);

-- Learning
create table explanation (
    id             varchar(36) primary key,
    key_id         varchar(36) not null references catalog_key (id) on delete cascade,
    level          varchar(10) not null,
    status         varchar(12) not null,
    markdown       text,
    revision       int         not null default 0,
    requested_by   varchar(60),
    failure_reason varchar(500),
    updated_at     timestamp with time zone not null,
    lock_version   bigint      not null default 0,
    constraint ux_explanation_key_level unique (key_id, level)
);

-- Discussion
create table discussion_thread (
    id           varchar(36)  primary key,
    key_id       varchar(36)  not null references catalog_key (id) on delete cascade,
    title        varchar(200) not null,
    opened_by    varchar(60)  not null,
    closed       boolean      not null default false,
    opened_at    timestamp with time zone not null,
    lock_version bigint       not null default 0
);
create index ix_thread_key on discussion_thread (key_id);

create table discussion_message (
    id            varchar(36) primary key,
    thread_id     varchar(36) not null references discussion_thread (id) on delete cascade,
    author        varchar(60) not null,
    from_ai_tutor boolean     not null,
    content       text        not null,
    posted_at     timestamp with time zone not null
);
create index ix_message_thread_time on discussion_message (thread_id, posted_at);

-- Progress (read model)
create table study_activity (
    id          varchar(36) primary key,
    learner     varchar(60) not null,
    key_id      varchar(36) not null,
    type        varchar(30) not null,
    occurred_at timestamp with time zone not null
);
create index ix_activity_learner on study_activity (learner, occurred_at);

-- Transactional outbox
create table outbox_event (
    id           varchar(36)  primary key,
    topic        varchar(100) not null,
    event_type   varchar(100) not null,
    aggregate_id varchar(36)  not null,
    payload      text         not null,
    occurred_at  timestamp with time zone not null,
    published_at timestamp with time zone
);
create index ix_outbox_unpublished on outbox_event (published_at, occurred_at);
