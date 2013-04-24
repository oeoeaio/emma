# --------------------------------------------------------
# Host:                         127.0.0.1
# Server version:               5.1.45-community
# Server OS:                    Win32
# HeidiSQL version:             6.0.0.3603
# Date/time:                    2011-05-19 12:22:49
# --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

# Dumping database structure for remp
CREATE DATABASE IF NOT EXISTS `remp` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `remp`;


# Dumping structure for table remp.broken_files
CREATE TABLE IF NOT EXISTS `broken_files` (
  `installation_id` tinyint(3) NOT NULL,
  `filename` varchar(17) NOT NULL,
  `error` varchar(10) NOT NULL,
  PRIMARY KEY (`installation_id`,`filename`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table remp.data_all
CREATE TABLE IF NOT EXISTS `data_all` (
  `record_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `installation_id` tinyint(3) unsigned NOT NULL,
  `date_time` datetime NOT NULL,
  `module_sn` varchar(10) NOT NULL,
  `reading_type` enum('WL','CT','PH','SA') NOT NULL,
  `channel` tinyint(3) unsigned NOT NULL,
  `raw` float DEFAULT NULL,
  `converted` float DEFAULT NULL,
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `record_id` (`record_id`),
  KEY `record_id_2` (`record_id`),
  KEY `installation_and_time` (`installation_id`,`date_time`),
  CONSTRAINT `FK_data_all_installations` FOREIGN KEY (`installation_id`) REFERENCES `installations` (`installation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table remp.data_missing
CREATE TABLE IF NOT EXISTS `data_missing` (
  `record_id` int(10) unsigned NOT NULL,
  `installation_id` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `fill_rule` smallint(4) unsigned zerofill DEFAULT NULL,
  PRIMARY KEY (`record_id`),
  KEY `FK_data_missing_installations` (`installation_id`),
  CONSTRAINT `FK_data_missing_installations` FOREIGN KEY (`installation_id`) REFERENCES `installations` (`installation_id`),
  CONSTRAINT `FK__data_all` FOREIGN KEY (`record_id`) REFERENCES `data_all` (`record_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table remp.file_process_log
CREATE TABLE IF NOT EXISTS `file_process_log` (
  `uid` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `installation_id` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `filename` varchar(17) NOT NULL,
  `date_time` datetime NOT NULL,
  PRIMARY KEY (`uid`),
  UNIQUE KEY `uid` (`uid`),
  KEY `uid_2` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table remp.filler_error
CREATE TABLE IF NOT EXISTS `filler_error` (
  `record_id` int(10) NOT NULL,
  `error_code` varchar(4) NOT NULL,
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table remp.header_log
CREATE TABLE IF NOT EXISTS `header_log` (
  `header_id` mediumint(5) unsigned NOT NULL AUTO_INCREMENT,
  `installation_id` tinyint(3) unsigned NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table remp.installations
CREATE TABLE IF NOT EXISTS `installations` (
  `installation_id` tinyint(3) unsigned NOT NULL AUTO_INCREMENT,
  `study_name` varchar(20) DEFAULT NULL,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `given_name_p` varchar(30) DEFAULT NULL,
  `surname_p` varchar(30) DEFAULT NULL,
  `given_name_s` varchar(30) DEFAULT NULL,
  `surname_s` varchar(30) DEFAULT NULL,
  `org` varchar(30) DEFAULT NULL,
  `address` varchar(30) DEFAULT NULL,
  `suburb` varchar(30) DEFAULT NULL,
  `state` varchar(3) DEFAULT NULL,
  `postcode` varchar(4) DEFAULT NULL,
  `phone` varchar(10) DEFAULT NULL,
  `mobile` varchar(10) DEFAULT NULL,
  `email` varchar(50) DEFAULT NULL,
  `hw_type` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT '0=instantaneous_gas, 1=gas_storage, 2=electric_storage',
  `notes` blob,
  PRIMARY KEY (`installation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table remp.lights
CREATE TABLE IF NOT EXISTS `lights` (
  `uid` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `installation_id` tinyint(3) unsigned NOT NULL,
  `item_identifier` varchar(4) NOT NULL,
  `switch_type` enum('GPO','Hard') NOT NULL,
  `switch_no` varchar(4) NOT NULL,
  `dimmer` enum('Yes','No') NOT NULL DEFAULT 'No',
  `motion` enum('Yes','No') NOT NULL DEFAULT 'No',
  `lamp_fitting` enum('Batton-GLS','Batton-Linear','Oyster','Pendant','Chandelier','Downlight','Spot','Uplight','Wall Light','Heat Lamp','','Nightlight','Desk Lamp','Table Lamp','Floor/Standard Lamp','Floodlight/Spot','Garden/Decorative','Rangehood','Other','Cannot identify') NOT NULL DEFAULT 'Cannot identify',
  `lamp_technology` enum('Incandescent - mains voltage','Halogen - mains voltage','Halogen - low voltage','Compact fluorescent - integral ballast','Compact fluorescent - separate ballast','Linear fluorescent','Circular fluorescent','LED','Heat Lamp','Other','Cannot identify','N/A') NOT NULL DEFAULT 'Cannot identify',
  `lamp_cap` enum('E14','E27','B15','B22','GU10','Other','Cannot identify','N/A') NOT NULL DEFAULT 'Cannot identify',
  `lamp_transformer` enum('Magnetic','Electronic','N/A','Cannot identify') NOT NULL DEFAULT 'N/A',
  `lamp_w_estimated` float unsigned DEFAULT NULL,
  `lamp_w_actual` float unsigned DEFAULT NULL,
  `usage` enum('Freq Long','Freq Short','Occassional','Rarely','N/A') NOT NULL DEFAULT 'N/A',
  `notes` blob,
  PRIMARY KEY (`uid`),
  UNIQUE KEY `uid` (`uid`),
  KEY `uid_2` (`uid`),
  KEY `FK_lights_installations` (`installation_id`),
  CONSTRAINT `FK_lights_installations` FOREIGN KEY (`installation_id`) REFERENCES `installations` (`installation_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.


# Dumping structure for table remp.log_reset_points
CREATE TABLE IF NOT EXISTS `log_reset_points` (
  `record_id` int(10) unsigned NOT NULL,
  `installation_id` tinyint(3) unsigned NOT NULL,
  `channel` tinyint(3) unsigned NOT NULL,
  `date_time` datetime NOT NULL,
  `raw` double NOT NULL,
  `reason` enum('CounterSlips','CounterJumps') NOT NULL,
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='Contains a log of points that have been manually reset';

# Data exporting was unselected.


# Dumping structure for table remp.missing_summary
CREATE TABLE IF NOT EXISTS `missing_summary` (
  `uid` int(10) NOT NULL AUTO_INCREMENT,
  `installation_id` tinyint(4) NOT NULL,
  `date_col` date NOT NULL,
  `channel` tinyint(4) NOT NULL,
  `missing` float DEFAULT NULL,
  `not_filled` float DEFAULT NULL,
  `not_converted` float DEFAULT NULL,
  PRIMARY KEY (`uid`),
  UNIQUE KEY `date_installation_id` (`installation_id`,`date_col`,`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

# Data exporting was unselected.
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
