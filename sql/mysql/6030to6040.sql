--20120214
CREATE TABLE `eip_t_timeline` (
   `timeline_id` int(11) NOT NULL AUTO_INCREMENT,
   `parent_id` int(11) NOT NULL DEFAULT 0,
   `owner_id` int(11),
   `note` text,
   `create_date` datetime DEFAULT NULL,
   `update_date` datetime DEFAULT NULL,
   PRIMARY KEY(`timeline_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
ALTER TABLE `eip_t_timeline` ADD FOREIGN KEY (  `owner_id` ) REFERENCES  `turbine_user` (`user_id`);
--20120214

--20120229
CREATE TABLE `eip_t_timeline_like` (
  `timeline_like_id` int(11) NOT NULL AUTO_INCREMENT,
  `timeline_id` int(11) NOT NULL,
  `owner_id` int(11) NOT NULL,
  PRIMARY KEY (`timeline_like_id`),
  UNIQUE KEY `eip_t_timeline_timelineid_ownerid_key` (`timeline_id`, `owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--20120229

--20120307
ALTER TABLE `eip_t_ext_timecard_system` ADD COLUMN `start_day` smallint;
UPDATE `eip_t_ext_timecard_system` SET start_day=1;

CREATE TABLE `eip_t_timeline_file`
(
    `file_id` int(11) NOT NULL AUTO_INCREMENT,
    `owner_id` int(11),
    `timeline_id` int(11),
    `file_name` varchar(128) NOT NULL,
    `file_path` text NOT NULL,
    `file_thumbnail` blob,
    `create_date` date DEFAULT NULL,
    `update_date` datetime DEFAULT NULL,
    FOREIGN KEY (`timeline_id`) REFERENCES `eip_t_timeline` (`timeline_id`) ON DELETE CASCADE,
    PRIMARY KEY (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--20120307