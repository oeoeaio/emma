# --------------------------------------------------------
# Host:                         127.0.0.1
# Server version:               5.1.45-community
# Server OS:                    Win32
# HeidiSQL version:             6.0.0.3603
# Date/time:                    2012-06-12 18:39:01
# --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

# Dumping structure for table enduse.data_sa
CREATE TABLE IF NOT EXISTS `data_sa` (
  `record_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `file_id` int(10) unsigned NOT NULL,
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
  `file_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `file_name` varchar(50) NOT NULL,
  `meter_sn` varchar(25) NOT NULL,
  `frequency` smallint(5) unsigned NOT NULL,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  PRIMARY KEY (`file_id`),
  KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `FK_files_sources` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

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


# Dumping structure for table enduse.issues
CREATE TABLE IF NOT EXISTS `issues` (
  `issue_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `start_date` datetime NOT NULL,
  `end_date` datetime NOT NULL,
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `issue_type` enum('MissingValue','MissingFile','OutOfRange','Conflict') NOT NULL,
  `urgency` enum('High','Moderate','Low') NOT NULL,
  PRIMARY KEY (`issue_id`),
  KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `FK_errors_sources` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table enduse.issues_broken_files
CREATE TABLE IF NOT EXISTS `issues_broken_files` (
  `issue_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `date_time` datetime NOT NULL,
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `issue_type` enum('MissingValue','MissingFile','OutOfRange','Duplicate') NOT NULL,
  PRIMARY KEY (`issue_id`),
  KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `issues_broken_files_ibfk_1` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.issues_files
CREATE TABLE IF NOT EXISTS `issues_files` (
  `issue_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `date_time` datetime NOT NULL,
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `issue_type` enum('MissingValue','MissingFile','OutOfRange','Duplicate') NOT NULL,
  PRIMARY KEY (`issue_id`),
  KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `issues_files_ibfk_1` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.issue_points
CREATE TABLE IF NOT EXISTS `issue_points` (
  `issue_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `site_id` smallint(5) unsigned NOT NULL,
  `source_id` smallint(5) unsigned NOT NULL,
  `date_time` datetime NOT NULL,
  `value` double DEFAULT NULL,
  `issue_type` enum('MissingValue','MissingFile','OutOfRange','Conflict') NOT NULL DEFAULT 'MissingValue',
  `date_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`issue_id`),
  KEY `site_id_source_id` (`site_id`,`source_id`,`date_time`),
  CONSTRAINT `issue_points_ibfk_1` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.


# Dumping structure for table enduse.lights
CREATE TABLE IF NOT EXISTS `lights` (
  `source_id` smallint(5) unsigned NOT NULL,
  `site_id` smallint(5) unsigned NOT NULL,
  `source_name` varchar(10) NOT NULL,
  `wattage` double unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`source_id`)
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


# Dumping structure for table enduse.ranges
CREATE TABLE IF NOT EXISTS `ranges` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `site_id` smallint(5) unsigned DEFAULT NULL,
  `source_id` smallint(5) unsigned DEFAULT NULL,
  `min` smallint(5) unsigned NOT NULL DEFAULT '0',
  `max` smallint(5) unsigned NOT NULL DEFAULT '3000',
  PRIMARY KEY (`id`),
  UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  CONSTRAINT `FK_ranges_sources` FOREIGN KEY (`site_id`, `source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table enduse.sites
CREATE TABLE IF NOT EXISTS `sites` (
  `site_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `site_name` varchar(10) DEFAULT NULL,
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
  `source_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `site_id` smallint(5) unsigned NOT NULL,
  `source_name` varchar(10) DEFAULT NULL,
  `source_type` enum('CD','CW','DV','DW','FN','FZ','GS','HT','HM','KT','LP','LT','MD','MT','OT','RH','RF','RT','ST','TP','TS','TV','VM','WT') NOT NULL,
  `measurement_type` enum('ActEnergy','AppEnergy','OnTime','Temp','Humidity','Pulse','ActPower','AppPower','LightLevel','Volts','Amps','AvgTemp') DEFAULT NULL,
  `brand` varchar(30) DEFAULT NULL,
  `model` varchar(30) DEFAULT NULL,
  `serial` varchar(30) DEFAULT NULL,
  `location` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`source_id`),
  UNIQUE KEY `site_id_source_name` (`site_id`,`source_name`),
  KEY `site_id` (`site_id`),
  CONSTRAINT `FK_sources_sites` FOREIGN KEY (`site_id`) REFERENCES `sites` (`site_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

# Data exporting was unselected.
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
