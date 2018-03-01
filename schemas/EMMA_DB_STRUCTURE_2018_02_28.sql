-- phpMyAdmin SQL Dump
-- version 4.6.6
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Generation Time: Feb 28, 2018 at 04:30 PM
-- Server version: 5.5.57-MariaDB
-- PHP Version: 5.6.31

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `enduse`
--

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `getRefrigAnalysisData` (IN `siteID` INT, IN `sourceID` INT, IN `fileID` BIGINT, IN `tempSourceID` INT, IN `startDate` DATETIME, IN `endDate` DATETIME)  BEGIN

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

SELECT UNIX_TIMESTAMP(data_sa.date_time) AS unix_ts,data_sa.value,temporary_temperatures.value AS temp FROM data_sa LEFT JOIN temporary_temperatures ON ROUND(UNIX_TIMESTAMP(data_sa.date_time)/tempFreq)*tempFreq = temporary_temperatures.unix_ts WHERE site_id = siteID AND source_id = sourceID AND file_id = fileID AND date_time BETWEEN startDate AND endDate;

DROP TEMPORARY TABLE `temporary_temperatures`;

END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `appliances`
--

CREATE TABLE `appliances` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `circuit_id` smallint(5) UNSIGNED DEFAULT NULL,
  `room_id` smallint(5) UNSIGNED DEFAULT NULL,
  `appliance_id` smallint(5) UNSIGNED NOT NULL,
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
  `on_w` double UNSIGNED DEFAULT NULL,
  `as_w` double UNSIGNED DEFAULT NULL,
  `ps_w` double UNSIGNED DEFAULT NULL,
  `off_w` double UNSIGNED DEFAULT NULL,
  `ds_w` double UNSIGNED DEFAULT NULL,
  `year_of_purchase` smallint(4) DEFAULT NULL,
  `usage_amount` double UNSIGNED DEFAULT NULL,
  `usage_units` tinytext,
  `feature1` tinytext,
  `feature2` tinytext,
  `feature3` tinytext,
  `feature4` tinytext,
  `feature5` tinytext,
  `notes` tinyblob
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `circuits`
--

CREATE TABLE `circuits` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL DEFAULT '0',
  `circuit_id` smallint(5) UNSIGNED NOT NULL,
  `notes` tinytext
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `data_sa`
--

CREATE TABLE `data_sa` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `file_id` int(10) UNSIGNED NOT NULL,
  `record_id` int(10) UNSIGNED NOT NULL,
  `date_time` datetime NOT NULL,
  `value` decimal(13,6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `files`
--

CREATE TABLE `files` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `file_id` int(10) UNSIGNED NOT NULL,
  `file_name` varchar(50) NOT NULL,
  `meter_sn` varchar(25) NOT NULL,
  `frequency` smallint(5) UNSIGNED NOT NULL,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `folder_name` tinytext NOT NULL,
  `file_size` int(11) NOT NULL,
  `date_modified` datetime NOT NULL,
  `update_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `gas`
--

CREATE TABLE `gas` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `gas_id` smallint(5) UNSIGNED NOT NULL,
  `notes` tinyblob
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `gpos`
--

CREATE TABLE `gpos` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `circuit_id` smallint(5) UNSIGNED DEFAULT NULL,
  `room_id` smallint(5) UNSIGNED DEFAULT NULL,
  `gpo_id` smallint(5) UNSIGNED NOT NULL,
  `gpo_name` varchar(16) DEFAULT NULL,
  `notes` tinytext
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `header_log`
--

CREATE TABLE `header_log` (
  `header_id` mediumint(5) UNSIGNED NOT NULL,
  `site_id` tinyint(3) UNSIGNED NOT NULL,
  `sub_period_id` smallint(5) UNSIGNED NOT NULL,
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
  `wl_sensor_chs` varchar(143) DEFAULT NULL COMMENT '8x(2x6)+8x(5)+7'
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `humidities`
--

CREATE TABLE `humidities` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `room_id` smallint(5) UNSIGNED DEFAULT NULL,
  `humidity_id` smallint(5) UNSIGNED NOT NULL,
  `notes` tinyblob
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `issues`
--

CREATE TABLE `issues` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `issue_id` int(10) UNSIGNED NOT NULL,
  `start_date` datetime NOT NULL,
  `end_date` datetime NOT NULL,
  `issue_type` enum('Conflict','MissingValue','OutOfRange','NoData') NOT NULL,
  `urgency` enum('High','Moderate','Low') NOT NULL,
  `mod_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `notes` tinytext
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `issues_files`
--

CREATE TABLE `issues_files` (
  `issue_id` int(10) UNSIGNED NOT NULL,
  `file_name` varchar(50) NOT NULL,
  `folder_name` varchar(50) NOT NULL,
  `issue_type` enum('Empty','Corrupt','MissHead','ModData','ConcData','NoSiteMatch','NoFileFreq','WRowMismatch','WColMismatch','CTRowMismatch','CTColMismatch','DuplicateCTName','PHRowMismatch','PHColMismatch','FileWriteError') NOT NULL,
  `file_size` int(10) UNSIGNED NOT NULL,
  `date_modified` datetime NOT NULL,
  `update_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `attempts` tinyint(3) UNSIGNED NOT NULL DEFAULT '1',
  `notes` tinytext
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `issues_sources`
--

CREATE TABLE `issues_sources` (
  `issue_id` int(10) UNSIGNED NOT NULL,
  `file_name` varchar(50) NOT NULL,
  `folder_name` varchar(50) NOT NULL,
  `issue_type` enum('InvalidSourceName','DuplicateWChName','DuplicateCTChName','SourceWriteError') NOT NULL,
  `source_name` varchar(16) NOT NULL,
  `update_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `attempts` tinyint(3) UNSIGNED NOT NULL DEFAULT '1',
  `notes` tinytext
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `issue_points`
--

CREATE TABLE `issue_points` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `issue_id` int(10) UNSIGNED NOT NULL,
  `record_id` int(10) UNSIGNED NOT NULL,
  `date_time` datetime NOT NULL,
  `value` double DEFAULT NULL,
  `issue_type` enum('Conflict','MissingValue','OutOfRange') NOT NULL DEFAULT 'MissingValue',
  `update_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `lights`
--

CREATE TABLE `lights` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `circuit_id` smallint(5) UNSIGNED DEFAULT NULL,
  `room_id` smallint(5) UNSIGNED DEFAULT NULL,
  `light_id` smallint(5) UNSIGNED NOT NULL,
  `wattage` double UNSIGNED DEFAULT '0',
  `notes` tinyblob
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `meters`
--

CREATE TABLE `meters` (
  `meter_sn` varchar(25) NOT NULL,
  `meter_type` enum('Energy','Temperature') NOT NULL,
  `current_site` smallint(5) UNSIGNED DEFAULT NULL,
  `date_installed` date DEFAULT NULL,
  `date_full` date DEFAULT NULL,
  `frequency` smallint(5) UNSIGNED DEFAULT NULL,
  `battery_installed` date DEFAULT NULL,
  `battery_replace` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `motion`
--

CREATE TABLE `motion` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `room_id` smallint(5) UNSIGNED DEFAULT NULL,
  `motion_id` smallint(5) UNSIGNED NOT NULL,
  `notes` tinyblob
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `phases`
--

CREATE TABLE `phases` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `phase_id` smallint(5) UNSIGNED NOT NULL,
  `notes` tinytext
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `ranges`
--

CREATE TABLE `ranges` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `range_id` int(10) UNSIGNED NOT NULL,
  `min` double NOT NULL DEFAULT '0',
  `max` double NOT NULL DEFAULT '2500'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `rooms`
--

CREATE TABLE `rooms` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `room_id` smallint(5) UNSIGNED NOT NULL,
  `room_number` tinyint(3) UNSIGNED NOT NULL,
  `room_type` enum('Bathroom','Bedroom','Dining','Foyer-inside','Garage','Hallway','Kitchen','Kitchen/Living','Laundry','Living-other','Lounge','Other-inside','Other-outside','Outside-general','Pantry','Storage Room','Study','Toilet','Verandah','Walk-in Robe') NOT NULL,
  `area` double DEFAULT NULL,
  `notes` tinytext
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `sites`
--

CREATE TABLE `sites` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `site_name` varchar(16) DEFAULT NULL,
  `concentrator` varchar(8) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `given_name` varchar(30) DEFAULT NULL,
  `surname` varchar(30) DEFAULT NULL,
  `suburb` varchar(30) DEFAULT NULL,
  `state` varchar(3) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `sources`
--

CREATE TABLE `sources` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `source_name` varchar(20) DEFAULT NULL,
  `source_type` enum('Appliance','Circuit','Gas','Humidity','Light','Motion','Phase','Temperature','Water') DEFAULT NULL,
  `measurement_type` enum('ActEnergy','AppEnergy','OnTime','Temp','Humidity','Pulse','ActPower','AppPower','LightLevel','Volts','Amps','AvgTemp') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `temp`
--

CREATE TABLE `temp` (
  `id` int(10) NOT NULL,
  `site_id` smallint(6) DEFAULT '0',
  `source_id` smallint(6) DEFAULT '0',
  `issue_type` tinytext
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `temperatures`
--

CREATE TABLE `temperatures` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `room_id` smallint(5) UNSIGNED DEFAULT NULL,
  `temperature_id` smallint(5) UNSIGNED NOT NULL,
  `notes` tinyblob
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `water`
--

CREATE TABLE `water` (
  `site_id` smallint(5) UNSIGNED NOT NULL,
  `source_id` smallint(5) UNSIGNED NOT NULL,
  `water_id` smallint(5) UNSIGNED NOT NULL,
  `notes` tinyblob
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `appliances`
--
ALTER TABLE `appliances`
  ADD PRIMARY KEY (`appliance_id`),
  ADD UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  ADD KEY `room_id` (`room_id`),
  ADD KEY `circuit_id` (`circuit_id`);

--
-- Indexes for table `circuits`
--
ALTER TABLE `circuits`
  ADD PRIMARY KEY (`circuit_id`),
  ADD UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`);

--
-- Indexes for table `data_sa`
--
ALTER TABLE `data_sa`
  ADD PRIMARY KEY (`record_id`),
  ADD UNIQUE KEY `site_id_source_id_date_time` (`site_id`,`source_id`,`date_time`),
  ADD KEY `site_id_source_id_file_id` (`site_id`,`source_id`,`file_id`);

--
-- Indexes for table `files`
--
ALTER TABLE `files`
  ADD PRIMARY KEY (`file_id`),
  ADD KEY `site_id_source_id` (`site_id`,`source_id`);

--
-- Indexes for table `gas`
--
ALTER TABLE `gas`
  ADD PRIMARY KEY (`gas_id`),
  ADD UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`);

--
-- Indexes for table `gpos`
--
ALTER TABLE `gpos`
  ADD PRIMARY KEY (`gpo_id`),
  ADD UNIQUE KEY `site_id_gpo_name` (`site_id`,`gpo_name`),
  ADD KEY `circuit_id` (`circuit_id`),
  ADD KEY `room_id` (`room_id`);

--
-- Indexes for table `header_log`
--
ALTER TABLE `header_log`
  ADD PRIMARY KEY (`header_id`);

--
-- Indexes for table `humidities`
--
ALTER TABLE `humidities`
  ADD PRIMARY KEY (`humidity_id`),
  ADD UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  ADD KEY `room_id` (`room_id`);

--
-- Indexes for table `issues`
--
ALTER TABLE `issues`
  ADD PRIMARY KEY (`issue_id`),
  ADD UNIQUE KEY `site_id_source_id_start_date` (`site_id`,`source_id`,`start_date`),
  ADD KEY `site_id_source_id` (`site_id`,`source_id`);

--
-- Indexes for table `issues_files`
--
ALTER TABLE `issues_files`
  ADD PRIMARY KEY (`issue_id`),
  ADD UNIQUE KEY `file_name_folder_name_issue_type` (`file_name`,`folder_name`,`issue_type`);

--
-- Indexes for table `issues_sources`
--
ALTER TABLE `issues_sources`
  ADD PRIMARY KEY (`issue_id`),
  ADD UNIQUE KEY `file_name_folder_name_issue_type_source_name` (`file_name`,`folder_name`,`issue_type`,`source_name`);

--
-- Indexes for table `issue_points`
--
ALTER TABLE `issue_points`
  ADD PRIMARY KEY (`issue_id`),
  ADD UNIQUE KEY `record_id_issue_type` (`record_id`,`issue_type`),
  ADD KEY `site_id_source_id` (`site_id`,`source_id`,`issue_type`,`date_time`);

--
-- Indexes for table `lights`
--
ALTER TABLE `lights`
  ADD PRIMARY KEY (`light_id`),
  ADD UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  ADD KEY `room_id` (`room_id`),
  ADD KEY `circuit_id` (`circuit_id`);

--
-- Indexes for table `meters`
--
ALTER TABLE `meters`
  ADD PRIMARY KEY (`meter_sn`),
  ADD KEY `current_site` (`current_site`);

--
-- Indexes for table `motion`
--
ALTER TABLE `motion`
  ADD PRIMARY KEY (`motion_id`),
  ADD UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  ADD KEY `room_id` (`room_id`);

--
-- Indexes for table `phases`
--
ALTER TABLE `phases`
  ADD PRIMARY KEY (`phase_id`),
  ADD UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`);

--
-- Indexes for table `ranges`
--
ALTER TABLE `ranges`
  ADD PRIMARY KEY (`range_id`),
  ADD UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`);

--
-- Indexes for table `rooms`
--
ALTER TABLE `rooms`
  ADD PRIMARY KEY (`room_id`),
  ADD UNIQUE KEY `site_id_room_number` (`site_id`,`room_number`);

--
-- Indexes for table `sites`
--
ALTER TABLE `sites`
  ADD PRIMARY KEY (`site_id`),
  ADD UNIQUE KEY `site_name` (`site_name`);

--
-- Indexes for table `sources`
--
ALTER TABLE `sources`
  ADD PRIMARY KEY (`source_id`),
  ADD UNIQUE KEY `site_id_source_name` (`site_id`,`source_name`),
  ADD KEY `site_id` (`site_id`);

--
-- Indexes for table `temp`
--
ALTER TABLE `temp`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `temperatures`
--
ALTER TABLE `temperatures`
  ADD PRIMARY KEY (`temperature_id`),
  ADD UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`),
  ADD KEY `room_id` (`room_id`);

--
-- Indexes for table `water`
--
ALTER TABLE `water`
  ADD PRIMARY KEY (`water_id`),
  ADD UNIQUE KEY `site_id_source_id` (`site_id`,`source_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `appliances`
--
ALTER TABLE `appliances`
  MODIFY `appliance_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `circuits`
--
ALTER TABLE `circuits`
  MODIFY `circuit_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `data_sa`
--
ALTER TABLE `data_sa`
  MODIFY `record_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `files`
--
ALTER TABLE `files`
  MODIFY `file_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `gas`
--
ALTER TABLE `gas`
  MODIFY `gas_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `gpos`
--
ALTER TABLE `gpos`
  MODIFY `gpo_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `header_log`
--
ALTER TABLE `header_log`
  MODIFY `header_id` mediumint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `humidities`
--
ALTER TABLE `humidities`
  MODIFY `humidity_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `issues`
--
ALTER TABLE `issues`
  MODIFY `issue_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `issues_files`
--
ALTER TABLE `issues_files`
  MODIFY `issue_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `issues_sources`
--
ALTER TABLE `issues_sources`
  MODIFY `issue_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `issue_points`
--
ALTER TABLE `issue_points`
  MODIFY `issue_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `lights`
--
ALTER TABLE `lights`
  MODIFY `light_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `motion`
--
ALTER TABLE `motion`
  MODIFY `motion_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `phases`
--
ALTER TABLE `phases`
  MODIFY `phase_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `ranges`
--
ALTER TABLE `ranges`
  MODIFY `range_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `rooms`
--
ALTER TABLE `rooms`
  MODIFY `room_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `sites`
--
ALTER TABLE `sites`
  MODIFY `site_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `sources`
--
ALTER TABLE `sources`
  MODIFY `source_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `temp`
--
ALTER TABLE `temp`
  MODIFY `id` int(10) NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `temperatures`
--
ALTER TABLE `temperatures`
  MODIFY `temperature_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `water`
--
ALTER TABLE `water`
  MODIFY `water_id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- Constraints for dumped tables
--

--
-- Constraints for table `appliances`
--
ALTER TABLE `appliances`
  ADD CONSTRAINT `FK_appliances_circuits` FOREIGN KEY (`circuit_id`) REFERENCES `circuits` (`circuit_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  ADD CONSTRAINT `FK_appliances_rooms` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  ADD CONSTRAINT `FK_appliances_sources` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `circuits`
--
ALTER TABLE `circuits`
  ADD CONSTRAINT `FK_circuits_sources` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `data_sa`
--
ALTER TABLE `data_sa`
  ADD CONSTRAINT `FK_data_all_files` FOREIGN KEY (`site_id`,`source_id`,`file_id`) REFERENCES `files` (`site_id`, `source_id`, `file_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `files`
--
ALTER TABLE `files`
  ADD CONSTRAINT `FK_files_sources` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `gas`
--
ALTER TABLE `gas`
  ADD CONSTRAINT `gas_ibfk_2` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `gpos`
--
ALTER TABLE `gpos`
  ADD CONSTRAINT `FK_gpos_circuits` FOREIGN KEY (`circuit_id`) REFERENCES `circuits` (`circuit_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  ADD CONSTRAINT `FK_gpos_rooms` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  ADD CONSTRAINT `gpos_ibfk_1` FOREIGN KEY (`site_id`) REFERENCES `sites` (`site_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `humidities`
--
ALTER TABLE `humidities`
  ADD CONSTRAINT `humidities_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  ADD CONSTRAINT `humidities_ibfk_2` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `issues`
--
ALTER TABLE `issues`
  ADD CONSTRAINT `FK_errors_sources` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `issue_points`
--
ALTER TABLE `issue_points`
  ADD CONSTRAINT `issue_points_ibfk_1` FOREIGN KEY (`record_id`) REFERENCES `data_sa` (`record_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `lights`
--
ALTER TABLE `lights`
  ADD CONSTRAINT `FK_lights_circuits` FOREIGN KEY (`circuit_id`) REFERENCES `circuits` (`circuit_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  ADD CONSTRAINT `FK_lights_rooms` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  ADD CONSTRAINT `FK_lights_sources` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `meters`
--
ALTER TABLE `meters`
  ADD CONSTRAINT `FK_meters_sites` FOREIGN KEY (`current_site`) REFERENCES `sites` (`site_id`) ON DELETE SET NULL ON UPDATE SET NULL;

--
-- Constraints for table `motion`
--
ALTER TABLE `motion`
  ADD CONSTRAINT `motion_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  ADD CONSTRAINT `motion_ibfk_2` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `phases`
--
ALTER TABLE `phases`
  ADD CONSTRAINT `phases_ibfk_1` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `ranges`
--
ALTER TABLE `ranges`
  ADD CONSTRAINT `FK_ranges_sources` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `rooms`
--
ALTER TABLE `rooms`
  ADD CONSTRAINT `FK_rooms_sites` FOREIGN KEY (`site_id`) REFERENCES `sites` (`site_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `sources`
--
ALTER TABLE `sources`
  ADD CONSTRAINT `FK_sources_sites` FOREIGN KEY (`site_id`) REFERENCES `sites` (`site_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `temperatures`
--
ALTER TABLE `temperatures`
  ADD CONSTRAINT `temperatures_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`room_id`) ON DELETE SET NULL ON UPDATE SET NULL,
  ADD CONSTRAINT `temperatures_ibfk_2` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `water`
--
ALTER TABLE `water`
  ADD CONSTRAINT `water_ibfk_1` FOREIGN KEY (`site_id`,`source_id`) REFERENCES `sources` (`site_id`, `source_id`) ON DELETE CASCADE ON UPDATE CASCADE;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
