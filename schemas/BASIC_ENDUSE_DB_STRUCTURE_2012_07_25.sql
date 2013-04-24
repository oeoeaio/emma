# --------------------------------------------------------
# Host:                         127.0.0.1
# Server version:               5.1.45-community
# Server OS:                    Win32
# HeidiSQL version:             6.0.0.3603
# Date/time:                    2012-07-26 12:21:34
# --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

# Dumping structure for table enduse.appliances
CREATE TABLE IF NOT EXISTS `appliances` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `circuit_id` smallint(5) unsigned DEFAULT NULL,
  `room_id` smallint(5) unsigned DEFAULT NULL,
  `appliance_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `appliance_group` enum('Air Conditioners','Computers and Peripherals','Cooking Appliances','External Power Supplies','Heating Appliances','Home Cleaning Aids','Home Entertainment','Monitoring and Continuous Appliances','Office Equipment','Other Audio','Personal Health and Hygiene Products','Set Top Boxes','Small Kitchen Appliances','Telephones','Televisions','Tools','Water Heaters','Whitegoods','Miscellaneous Appliances') DEFAULT NULL,
  `appliance_type` tinytext,
  `brand` tinytext,
  `model` tinytext,
  `serial_no` tinytext,
  `connection_type` enum('Plug','Hardwired') DEFAULT NULL,
  `control` enum('Analogue','Electronic','None') DEFAULT NULL,
  `switch_type` enum('Off','Standby','None') DEFAULT NULL,
  `display` enum('Yes','No') DEFAULT NULL,
  `eps` enum('Yes','No') DEFAULT NULL,
  `delay_start` enum('Yes','No') DEFAULT NULL,
  `on_w` double unsigned DEFAULT NULL,
  `as_w` double unsigned DEFAULT NULL,
  `ps_w` double unsigned DEFAULT NULL,
  `off_w` double unsigned DEFAULT NULL,
  `ds_w` double unsigned DEFAULT NULL,
  `year_of_purchase` smallint(4) DEFAULT NULL,
  `usage_amount` double unsigned DEFAULT NULL,
  `usage_units` tinytext,
  `feature1` tinytext,
  `feature2` tinytext,
  `feature3` tinytext,
  `feature4` tinytext,
  `feature5` tinytext,
  `notes` tinyblob,
  PRIMARY KEY (`appliance_id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  KEY `room_id` (`room_id`),
  KEY `circuit_id` (`circuit_id`),
  CONSTRAINT `FK_appliances_circuits` FOREIGN KEY (`circuit_id`) REFERENCES `circuits` (`circuit_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `FK_appliances_rooms` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `FK_appliances_sources` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table enduse.circuits
CREATE TABLE IF NOT EXISTS `circuits` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL DEFAULT '0',
  `circuit_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `notes` tinytext,
  PRIMARY KEY (`circuit_id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `FK_circuits_sources` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.data_sa
CREATE TABLE IF NOT EXISTS `data_sa` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `file_id` int(10) unsigned NOT NULL,
  `record_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `date_time` datetime NOT NULL,
  `value` decimal(10,1) DEFAULT NULL,
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `site_id_source_id_date_time` (`site_id`,`source_id`,`date_time`),
  KEY `site_id_source_id_file_id` (`site_id`,`source_id`,`file_id`),
  CONSTRAINT `FK_data_all_files` FOREIGN KEY (`site_id`, `source_id`, `file_id`) REFERENCES `files` (`site_id`, `source_id`, `file_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.files
CREATE TABLE IF NOT EXISTS `files` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `file_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `file_name` varchar(50) NOT NULL,
  `meter_sn` varchar(25) NOT NULL,
  `frequency` smallint(5) unsigned NOT NULL,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `folder_name` tinytext NOT NULL,
  `file_size` int(11) NOT NULL,
  `date_modified` datetime NOT NULL,
  `update_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`),
  KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `FK_files_sources` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table enduse.gas
CREATE TABLE IF NOT EXISTS `gas` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `gas_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `notes` tinyblob,
  PRIMARY KEY (`gas_id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `gas_ibfk_2` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for procedure enduse.getRefrigAnalysisData
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `getRefrigAnalysisData`(IN `siteID` INT, IN `sourceID` INT, IN `tempSourceID` INT, IN `startDate` DATETIME, IN `endDate` DATETIME)
BEGIN

DECLARE tempFreq INT DEFAULT (SELECT MAX(frequency) FROM files WHERE file_id IN (SELECT DISTINCT file_id FROM data_sa WHERE site_id = siteID AND source_id = tempSourceID AND data_sa.date_time BETWEEN startDate AND endDate));

DROP TEMPORARY TABLE IF EXISTS `temporary_temperatures`;

CREATE TEMPORARY TABLE IF NOT EXISTS temporary_temperatures(
`unix_ts` INT NOT NULL,
`value` DECIMAL(10,1) NULL DEFAULT NULL,
INDEX `date_time` (`unix_ts`)
)
COLLATE='latin1_swedish_ci'
ENGINE=InnoDB
ROW_FORMAT=COMPACT;

INSERT INTO temporary_temperatures (SELECT UNIX_TIMESTAMP(date_time),value FROM data_sa WHERE site_id = siteID AND source_id = tempSourceID AND date_time BETWEEN startDate AND endDate);

SELECT UNIX_TIMESTAMP(data_sa.date_time) AS unix_ts,data_sa.value,temporary_temperatures.value AS temp FROM data_sa LEFT JOIN temporary_temperatures ON ROUND(UNIX_TIMESTAMP(data_sa.date_time)/tempFreq)*tempFreq = temporary_temperatures.unix_ts WHERE site_id = siteID AND source_id = sourceID AND date_time BETWEEN startDate AND endDate;

DROP TEMPORARY TABLE `temporary_temperatures`;

END//
DELIMITER ;


# Dumping structure for table enduse.gpos
CREATE TABLE IF NOT EXISTS `gpos` (
  `site_id` smallint(5) unsigned NOT NULL,
  `circuit_id` smallint(5) unsigned DEFAULT NULL,
  `room_id` smallint(5) unsigned DEFAULT NULL,
  `gpo_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `gpo_name` varchar(16) DEFAULT NULL,
  `notes` tinytext,
  PRIMARY KEY (`gpo_id`),
  UNIQUE KEY `site_id_gpo_name` (`site_id`,`gpo_name`),
  KEY `circuit_id` (`circuit_id`),
  KEY `room_id` (`room_id`),
  CONSTRAINT `FK_gpos_circuits` FOREIGN KEY (`circuit_id`) REFERENCES `circuits` (`circuit_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `FK_gpos_rooms` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `gpos_ibfk_1` FOREIGN KEY (`site_id`) REFERENCES `sites` (`site_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.header_log
CREATE TABLE IF NOT EXISTS `header_log` (
  `header_id` mediumint(5) unsigned NOT NULL AUTO_INCREMENT,
  `site_id` tinyint(3) unsigned NOT NULL,
  `sub_period_id` smallint(5) unsigned NOT NULL,
  `date_time` datetime DEFAULT NULL,
  `conc_name` varchar(16) DEFAULT NULL,
  `conc_sn` varchar(10) DEFAULT NULL,
  `conc_period` varchar(4) DEFAULT NULL,
  `conc_nbmod` varchar(2) DEFAULT NULL,
  `conc_version` varchar(2) DEFAULT NULL,
  `conc_winter` varchar(2) DEFAULT NULL,
  `ct_sns` varchar(44) DEFAULT NULL COMMENT '5x8+4',
  `ct_versions` varchar(14) DEFAULT NULL COMMENT '5x2+4',
  `ct_ch_names` varchar(509) DEFAULT NULL COMMENT '5x(16x6)+5x(5)+4',
  `ct_muls` varchar(89) DEFAULT NULL COMMENT '5x(2x6)+5x(5)+4',
  `ct_divs` varchar(89) DEFAULT NULL COMMENT '5x(2x6)+5x(5)+4',
  `ct_phases` varchar(89) DEFAULT NULL COMMENT '5x(2x6)+5x(5)+4',
  `wl_sn` varchar(10) DEFAULT NULL,
  `wl_version` varchar(2) DEFAULT NULL,
  `wl_ch_names` varchar(815) DEFAULT NULL COMMENT '8x(16x6)+8x(5)+7',
  `wl_sensor_sns` varchar(239) DEFAULT NULL COMMENT '8x(4x6)+8x(5)+7',
  `wl_sensor_chs` varchar(143) DEFAULT NULL COMMENT '8x(2x6)+8x(5)+7',
  PRIMARY KEY (`header_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.humidities
CREATE TABLE IF NOT EXISTS `humidities` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `room_id` smallint(5) unsigned DEFAULT NULL,
  `humidity_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `notes` tinyblob,
  PRIMARY KEY (`humidity_id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  KEY `room_id` (`room_id`),
  CONSTRAINT `humidities_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `humidities_ibfk_2` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.issues
CREATE TABLE IF NOT EXISTS `issues` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `issue_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `start_date` datetime NOT NULL,
  `end_date` datetime NOT NULL,
  `issue_type` enum('MissingValue','MissingFile','OutOfRange','Conflict') NOT NULL,
  `urgency` enum('High','Moderate','Low') NOT NULL,
  `mod_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `notes` tinytext,
  PRIMARY KEY (`issue_id`),
  UNIQUE KEY `site_id_source_id_start_date` (`site_id`,`source_id`,`start_date`),
  KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `FK_errors_sources` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table enduse.issues_files
CREATE TABLE IF NOT EXISTS `issues_files` (
  `issue_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `file_name` varchar(50) NOT NULL,
  `folder_name` varchar(50) NOT NULL,
  `issue_type` enum('Empty','Corrupt','MissHead','ModData','ConcData','NoSiteMatch','NoFileFreq','WRowMismatch','WColMismatch','CTRowMismatch','CTColMismatch','DuplicateCTName','PHRowMismatch','PHColMismatch','FileWriteError') NOT NULL,
  `mod_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `attempts` tinyint(3) unsigned NOT NULL DEFAULT '1',
  `notes` tinytext,
  PRIMARY KEY (`issue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.issues_sources
CREATE TABLE IF NOT EXISTS `issues_sources` (
  `issue_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `file_name` varchar(50) NOT NULL,
  `folder_name` varchar(50) NOT NULL,
  `issue_type` enum('DuplicateWChName','SourceWriteError') NOT NULL,
  `mod_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `attempts` tinyint(3) unsigned NOT NULL DEFAULT '1',
  `notes` tinytext,
  PRIMARY KEY (`issue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for event enduse.issue_aggregator
DELIMITER //
CREATE EVENT `issue_aggregator` ON SCHEDULE EVERY 1 DAY STARTS '2012-07-24 15:27:37' ON COMPLETION NOT PRESERVE ENABLE DO BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE last_mod_date DATETIME;
  DECLARE start_row_num,end_row_num INT DEFAULT 0;
  DECLARE curr_site_id,curr_source_id INT DEFAULT NULL;
  DECLARE curr_start_date,curr_end_date DATETIME DEFAULT NULL;
  DECLARE curr_issue_type ENUM('MissingData','OutOfRange','Conflict');
  #DECLARE file_cursor CURSOR FOR SELECT site_id,source_id,start_date,end_date FROM files WHERE mod_date >= (SELECT MAX(mod_date) FROM issues) ORDER BY site_id,source_id,start_date;

  DECLARE issue_cursor CURSOR FOR SELECT site_id,source_id,issue_type FROM issue_points GROUP BY site_id,source_id,issue_type ORDER BY site_id,source_id,issue_type;
  DECLARE issue_groups CURSOR FOR SELECT start_dates.date_time AS start_date,end_dates.date_time AS end_date FROM (SELECT @start_row_num:=@start_row_num+1 AS row_num,date_time FROM issue_points WHERE date_time NOT IN (SELECT DATE_ADD(issue_points.date_time,INTERVAL files.frequency/60 MINUTE) FROM issue_points LEFT JOIN data_sa ON issue_points.site_id = data_sa.site_id AND issue_points.source_id = data_sa.source_id AND issue_points.date_time = data_sa.date_time LEFT JOIN files ON data_sa.file_id = files.file_id WHERE issue_points.site_id = curr_site_id AND issue_points.source_id = curr_source_id AND issue_points.issue_type = curr_issue_type) AND issue_points.site_id = curr_site_id AND issue_points.source_id = curr_source_id AND issue_points.issue_type = curr_issue_type) AS start_dates LEFT JOIN (SELECT @end_row_num:=@end_row_num+1 AS row_num,date_time FROM issue_points WHERE date_time NOT IN (SELECT DATE_SUB(issue_points.date_time,INTERVAL files.frequency/60 MINUTE) FROM issue_points LEFT JOIN data_sa ON issue_points.site_id = data_sa.site_id AND issue_points.source_id = data_sa.source_id AND issue_points.date_time = data_sa.date_time LEFT JOIN files ON data_sa.file_id = files.file_id WHERE issue_points.site_id = curr_site_id AND issue_points.source_id = curr_source_id AND issue_points.issue_type = curr_issue_type) AND issue_points.site_id = curr_site_id AND issue_points.source_id = curr_source_id AND issue_points.issue_type = curr_issue_type) AS end_dates USING(row_num);
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
  
  
  
	
	OPEN issue_cursor;
	issue_loop: LOOP
		FETCH issue_cursor INTO curr_site_id,curr_source_id,curr_issue_type;
		IF done THEN
			LEAVE issue_loop;
		END IF;
		SET last_mod_date = (SELECT IFNULL(MAX(mod_date),0) FROM issues WHERE site_id = curr_site_id AND source_id = curr_source_id AND issue_type = curr_issue_type);		
		SET start_row_num = 0;
		SET end_row_num = 0;
		
		OPEN issue_groups;
		issue_group_loop: LOOP
			FETCH issue_groups INTO curr_start_date,curr_end_date;
			IF done THEN
				CLOSE issue_groups;
				SET done = FALSE;
				LEAVE issue_loop;
			END IF;
			REPLACE INTO issues (site_id,source_id,start_date,end_date,issue_type) VALUES(curr_site_id,curr_source_id,curr_start_date,curr_end_date,curr_issue_type);
			
			
		END LOOP;
		
	END LOOP;
	
	CLOSE issue_cursor;
	
	SET done = FALSE; # reset for next cursor loop
	
		/*IF prev_end_date IS NOT NULL AND curr_site_id = prev_site_id AND curr_source_id = prev_source_id AND curr_start_date <> THEN
			
	    	
	    	SET block_start = start_date;
		ELSE
		  	SET block_end = end_date;
		END
		SET prev_site_id = curr_site_id;
		SET prev_source_id = curr_source_id;
		SET prev_start_date = curr_start_date;
		SET prev_end_date = curr_end_date;*/
		
	#update missing data 	
	
	#always look through preious two days
	#then update days covered by files added since issues were last updated
	
/*	
	OPEN file_cursor;

	file_loop: LOOP
		FETCH file_cursor INTO curr_site_id,curr_source_id,curr_start_date,curr_end_date;
		IF done THEN
			LEAVE file_loop;
		END IF;
		
		
	END LOOP;

	CLOSE file_cursor;

*/

END//
DELIMITER ;


# Dumping structure for table enduse.issue_points
CREATE TABLE IF NOT EXISTS `issue_points` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `issue_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `date_time` datetime NOT NULL,
  `value` double DEFAULT NULL,
  `issue_type` enum('MissingValue','OutOfRange','Conflict') NOT NULL DEFAULT 'MissingValue',
  `date_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`issue_id`),
  KEY `site_id_source_id` (`site_id`,`source_id`,`date_time`),
  CONSTRAINT `issue_points_ibfk_1` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.lights
CREATE TABLE IF NOT EXISTS `lights` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `circuit_id` smallint(5) unsigned DEFAULT NULL,
  `room_id` smallint(5) unsigned DEFAULT NULL,
  `light_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `wattage` double unsigned DEFAULT '0',
  `notes` tinyblob,
  PRIMARY KEY (`light_id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  KEY `room_id` (`room_id`),
  KEY `circuit_id` (`circuit_id`),
  CONSTRAINT `FK_lights_circuits` FOREIGN KEY (`circuit_id`) REFERENCES `circuits` (`circuit_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `FK_lights_rooms` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `FK_lights_sources` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table enduse.meters
CREATE TABLE IF NOT EXISTS `meters` (
  `meter_sn` varchar(25) NOT NULL,
  `meter_type` enum('Energy','Temperature') NOT NULL,
  `current_site` smallint(5) unsigned DEFAULT NULL,
  `date_installed` date DEFAULT NULL,
  `date_full` date DEFAULT NULL,
  `frequency` smallint(5) unsigned DEFAULT NULL,
  `battery_installed` date DEFAULT NULL,
  `battery_replace` date DEFAULT NULL,
  PRIMARY KEY (`meter_sn`),
  KEY `current_site` (`current_site`),
  CONSTRAINT `FK_meters_sites` FOREIGN KEY (`current_site`) REFERENCES `sites` (`site_id`) ON DELETE SET NULL ON UPDATE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table enduse.motion
CREATE TABLE IF NOT EXISTS `motion` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `room_id` smallint(5) unsigned DEFAULT NULL,
  `motion_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `notes` tinyblob,
  PRIMARY KEY (`motion_id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  KEY `room_id` (`room_id`),
  CONSTRAINT `motion_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `motion_ibfk_2` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.phases
CREATE TABLE IF NOT EXISTS `phases` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `phase_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `notes` tinytext,
  PRIMARY KEY (`phase_id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `phases_ibfk_1` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.ranges
CREATE TABLE IF NOT EXISTS `ranges` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `range_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `min` double NOT NULL DEFAULT '0',
  `max` double NOT NULL DEFAULT '2500',
  PRIMARY KEY (`range_id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `FK_ranges_sources` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table enduse.rooms
CREATE TABLE IF NOT EXISTS `rooms` (
  `site_id` smallint(5) unsigned NOT NULL,
  `room_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `room_number` tinyint(3) unsigned NOT NULL,
  `room_type` enum('Bathroom','Bedroom','Dining','Foyer-inside','Garage','Hallway','Kitchen','Kitchen/Living','Laundry','Living-other','Lounge','Other-inside','Other-outside','Outside-general','Pantry','Storage Room','Study','Toilet','Verandah','Walk-in Robe') NOT NULL,
  `area` double DEFAULT NULL,
  `notes` tinytext,
  PRIMARY KEY (`room_id`),
  UNIQUE KEY `site_id_room_number` (`site_id`,`room_number`),
  CONSTRAINT `FK_rooms_sites` FOREIGN KEY (`site_id`) REFERENCES `sites` (`site_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table enduse.sites
CREATE TABLE IF NOT EXISTS `sites` (
  `site_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `site_name` varchar(16) DEFAULT NULL,
  `concentrator` varchar(8) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `given_name` varchar(30) DEFAULT NULL,
  `surname` varchar(30) DEFAULT NULL,
  `suburb` varchar(30) DEFAULT NULL,
  `state` varchar(3) DEFAULT NULL,
  PRIMARY KEY (`site_id`),
  UNIQUE KEY `site_name` (`site_name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.sources
CREATE TABLE IF NOT EXISTS `sources` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `source_name` varchar(16) DEFAULT NULL,
  `source_type` enum('Appliance','Circuit','Gas','Humidity','Light','Motion','Phase','Temperature','Water') DEFAULT NULL,
  `measurement_type` enum('ActEnergy','AppEnergy','OnTime','Temp','Humidity','Pulse','ActPower','AppPower','LightLevel','Volts','Amps','AvgTemp') DEFAULT NULL,
  PRIMARY KEY (`source_id`),
  UNIQUE KEY `site_id_source_name` (`site_id`,`source_name`),
  KEY `site_id` (`site_id`),
  CONSTRAINT `FK_sources_sites` FOREIGN KEY (`site_id`) REFERENCES `sites` (`site_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.temp
CREATE TABLE IF NOT EXISTS `temp` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) DEFAULT '0',
  `source_id` smallint(6) DEFAULT '0',
  `issue_type` tinytext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table enduse.temperatures
CREATE TABLE IF NOT EXISTS `temperatures` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `room_id` smallint(5) unsigned DEFAULT NULL,
  `temperature_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `notes` tinyblob,
  PRIMARY KEY (`temperature_id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  KEY `room_id` (`room_id`),
  CONSTRAINT `temperatures_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  CONSTRAINT `temperatures_ibfk_2` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for procedure enduse.test_issue_aggregator
DELIMITER //
CREATE DEFINER=`root`@`localhost` PROCEDURE `test_issue_aggregator`()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE last_mod_date DATETIME;
  DECLARE start_row_num,end_row_num INT DEFAULT 0;
  DECLARE curr_site_id,curr_source_id INT DEFAULT NULL;
  DECLARE curr_start_date,curr_end_date DATETIME DEFAULT NULL;
  DECLARE curr_issue_type ENUM('MissingValue','OutOfRange','Conflict');
  #DECLARE file_cursor CURSOR FOR SELECT site_id,source_id,start_date,end_date FROM files WHERE mod_date >= (SELECT MAX(mod_date) FROM issues) ORDER BY site_id,source_id,start_date;

  DECLARE issue_cursor CURSOR FOR SELECT site_id,source_id,issue_type FROM issue_points GROUP BY site_id,source_id,issue_type ORDER BY site_id,source_id,issue_type;
  DECLARE issue_groups CURSOR FOR SELECT start_dates.date_time AS start_date,end_dates.date_time AS end_date FROM (SELECT @start_row_num:=@start_row_num+1 AS row_num,date_time FROM issue_points WHERE date_time NOT IN (SELECT DATE_ADD(issue_points.date_time,INTERVAL files.frequency/60 MINUTE) FROM issue_points LEFT JOIN data_sa ON issue_points.site_id = data_sa.site_id AND issue_points.source_id = data_sa.source_id AND issue_points.date_time = data_sa.date_time LEFT JOIN files ON data_sa.file_id = files.file_id WHERE issue_points.site_id = curr_site_id AND issue_points.source_id = curr_source_id AND issue_points.issue_type = curr_issue_type) AND issue_points.site_id = curr_site_id AND issue_points.source_id = curr_source_id AND issue_points.issue_type = curr_issue_type) AS start_dates LEFT JOIN (SELECT @end_row_num:=@end_row_num+1 AS row_num,date_time FROM issue_points WHERE date_time NOT IN (SELECT DATE_SUB(issue_points.date_time,INTERVAL files.frequency/60 MINUTE) FROM issue_points LEFT JOIN data_sa ON issue_points.site_id = data_sa.site_id AND issue_points.source_id = data_sa.source_id AND issue_points.date_time = data_sa.date_time LEFT JOIN files ON data_sa.file_id = files.file_id WHERE issue_points.site_id = curr_site_id AND issue_points.source_id = curr_source_id AND issue_points.issue_type = curr_issue_type) AND issue_points.site_id = curr_site_id AND issue_points.source_id = curr_source_id AND issue_points.issue_type = curr_issue_type) AS end_dates USING(row_num);
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  
	
	OPEN issue_cursor;
	issue_loop: LOOP
		FETCH issue_cursor INTO curr_site_id,curr_source_id,curr_issue_type;
		IF done THEN
			LEAVE issue_loop;
		END IF;
		#SET last_mod_date = (SELECT IFNULL(MAX(mod_date),0) FROM issues WHERE site_id = curr_site_id AND source_id = curr_source_id AND issue_type = curr_issue_type);		
		SET start_row_num = 0;
		SET end_row_num = 0;
		INSERT INTO temp (site_id,source_id,issue_type) VALUES(curr_site_id,curr_source_id,curr_issue_type);
		
		OPEN issue_groups;
		issue_group_loop: LOOP
			FETCH issue_groups INTO curr_start_date,curr_end_date;
			IF done THEN
				CLOSE issue_groups;
				SET done = FALSE;
				LEAVE issue_group_loop;
			END IF;
			REPLACE INTO issues (site_id,source_id,start_date,end_date,issue_type) VALUES(curr_site_id,curr_source_id,curr_start_date,curr_end_date,curr_issue_type);
		END LOOP;
		
	END LOOP;
	
	CLOSE issue_cursor;
	
	SET done = FALSE; # reset for next cursor loop
	
		/*IF prev_end_date IS NOT NULL AND curr_site_id = prev_site_id AND curr_source_id = prev_source_id AND curr_start_date <> THEN
			
	    	
	    	SET block_start = start_date;
		ELSE
		  	SET block_end = end_date;
		END
		SET prev_site_id = curr_site_id;
		SET prev_source_id = curr_source_id;
		SET prev_start_date = curr_start_date;
		SET prev_end_date = curr_end_date;*/
		
	#update missing data 	
	
	#always look through preious two days
	#then update days covered by files added since issues were last updated
	
/*	
	OPEN file_cursor;

	file_loop: LOOP
		FETCH file_cursor INTO curr_site_id,curr_source_id,curr_start_date,curr_end_date;
		IF done THEN
			LEAVE file_loop;
		END IF;
		
		
	END LOOP;

	CLOSE file_cursor;

*/

END//
DELIMITER ;


# Dumping structure for table enduse.water
CREATE TABLE IF NOT EXISTS `water` (
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `water_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `notes` tinyblob,
  PRIMARY KEY (`water_id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `water_ibfk_1` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
