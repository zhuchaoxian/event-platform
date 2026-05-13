create table if not exists event_archive (
    id bigint not null auto_increment primary key,
    event_id varchar(64) not null,
    device_id varchar(64) not null,
    event_timestamp bigint not null,
    event_type varchar(64) not null,
    payload_json json null,
    created_at datetime not null,
    key idx_event_archive_device_time (device_id, event_timestamp),
    unique key uk_event_archive_event_id (event_id)
) engine=InnoDB default charset=utf8mb4;

create table if not exists db_camera (
    id bigint not null auto_increment primary key,
    camera_id varchar(64) not null,
    camera_name varchar(255) null,
    latitude double null,
    longitude double null,
    status int null,
    create_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    unique key uk_camera_info_camera_id (camera_id),
    key idx_camera_info_ts (create_at)
) engine=InnoDB default charset=utf8mb4;
