CREATE DATABASE vgatedb DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE vgatedb;

DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `createtask`;

CREATE TABLE `createtask` (
  `TASK_ID` varchar(36) NOT NULL,
  `GOLD_UUID` varchar(36) default NULL,
  `HOST_UUID` varchar(36) default NULL,
  `HOST_IP` varchar(15) default NULL,
  `HOST_USERNAME` varchar(20) default NULL,
  `HOST_PASSWORD` varchar(50) default NULL,
  `TASK_STATUS` int(11) default NULL,
  `TASK_COUNT` int(11) default NULL,
  `RETRY_COUNT` int(11) default NULL,
  `VMMEMORY` int(11) default NULL,
  `SYSDISKSIZE` int(11) default NULL,
  `DATADISKSIZE` int(11) default NULL,
  `SOCKETCOUNT` int(11) default NULL,
  `CPUCOUNT` int(11) default NULL,
  `POLICY` int(11) default NULL,
  PRIMARY KEY  (`TASK_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `publishtask`;

CREATE TABLE `publishtask` (
  `TASK_ID` varchar(36) NOT NULL,
  `HOST_UUID` varchar(36) NOT NULL,
  `HOST_IP` varchar(15) NOT NULL,
  `HOST_USERNAME` varchar(20) NOT NULL,
  `HOST_PASSWORD` varchar(50) NOT NULL,
  `TASK_STATUS` int(1) NOT NULL,
  `SOURCE_TEMPLATE_UUID` varchar(36) default NULL,
  `GOLD_TEMPLATE_UUID` varchar(36) default NULL,
  `RETRY_COUNT` int(1) default '0',
  `TEMPLATE_SYSTEM_VDI_TAG` varchar(15) NOT NULL,
  `TEMPLATE_USER_VDI_TAG` varchar(15) NOT NULL,
  PRIMARY KEY  (`TASK_ID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `template`;

CREATE TABLE `template` (
  `TEMPLATE_UUID` varchar(36) NOT NULL,
  `GOLD_TEMPLATE_UUID` varchar(36) NOT NULL,
  `HOST_UUID` varchar(36) NOT NULL,
  `HOST_IP` varchar(15) NOT NULL,
  `HOST_USERNAME` varchar(20) NOT NULL,
  `HOST_PASSWORD` varchar(50) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8;