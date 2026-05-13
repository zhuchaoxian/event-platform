create table event_archive (
    id bigint auto_increment primary key,
    event_id varchar(64) not null,
    device_id varchar(64) not null,
    event_timestamp bigint not null,
    event_type varchar(64) not null,
    payload_json varchar(4096),
    created_at timestamp not null
);

create table db_camera (
    id bigint auto_increment primary key,
    camera_id varchar(64) not null,
    camera_name varchar(255),
    latitude double,
    longitude double,
    status int,
    create_at timestamp not null,
    updated_at timestamp not null,
    unique (camera_id)
);
