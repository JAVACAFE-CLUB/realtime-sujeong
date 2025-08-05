// schema.sql
create table if not exists person
(
    person_id   bigint  primary key auto_increment  not null,
    name        varchar(20),
    job         varchar(20),
    age         int,
    hobby       varchar(20)
)