# i2b2-SHRINE-QEP-cell
Source code for an i2b2 cell that triggers communication with an existing SHRINE hub

** Prerequisits: **
This code is designed to work on i2b2 1.8.1a instance of i2b2. i2b2 1.8.1a should be fully installed before applying these changes. 
The source code for 1.8.1a can be downloaded from: https://github.com/i2b2/i2b2-core-server/releases/tag/v1.8.1a.0001
i2b2 1.8.1a binaries and database code can be downloaded here: https://www.i2b2.org/software/download.html?d=465 and https://github.com/i2b2/i2b2-data/tree/v1.8.1a.0001

** Installation Instructions: **

** WAR file: **

This installation requires rebuilding the i2b2 war file. We will create a new SHRINE Cell and add a class to the CRC cell to initialize SHRINE queries from the webclient.
Steps to rebuild WAR file
1. Download Source code
2. Copy edu.harvard.i2b2.SHRINEQEP folder into i2b2 code directory
3. Copy edu.harvard.i2b2.crc\src\server\edu\harvard\i2b2\crc\dao\setfinder\QueryResultSHRINEBreakdownGenerator.java into the edu.harvard.i2b2.crc\src\server\edu\harvard\i2b2\crc\dao\setfinder folder in the i2b2 source code.
4. Navigate into edu.harvard.i2b2.crc folder
5. Run "..\apache-ant-1.10.7\bin\ant.bat -f .\master_build.xml clean build-all deploy" to build the CRC cell
6. Navigate to edu.harvard.i2b2.SHRINEQEP
7. Run "..\apache-ant-1.10.7\bin\ant.bat -f .\master_build.xml clean build-all deploy" to build the SHRINE cell

** Hive database: **
There are no changes to the hive schema
SHRINE Cell configuation is performed through rows in the hive_cell_params table. Update values in the below queries and execute them.

declare @i int
select @i = max(ID) from hive_cell_params
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 1, 'U', 'SHRINE', 'keystorePath', 'D:/i2b2/wildfly-17.0.1.Final/keytool/keystore.ks', 'A')
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 2, 'U', 'SHRINE', 'keystorePassphrase', '***********', 'A')
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 3, 'U', 'SHRINE', 'qepQueueName', 'i2b2devqep', 'A')
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 4, 'U', 'SHRINE', 'crcDatabaseType', 'SQLSERVER', 'A')
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 5, 'U', 'SHRINE', 'hubURL', 'https://shrine-hub.example.com:6443/shrine-api', 'A')
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 6, 'U', 'SHRINE', 'queryWaitTime', '240', 'A')
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 7, 'U', 'SHRINE', 'qepDataLookup', 'https://shrine-hub.example.com:6443/shrine-api/hub/node/i2b2-dev-qep', 'A')
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 8, 'U', 'SHRINE', 'dataSourceName', 'SHRINEDemoDS', 'A')
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 9, 'U', 'SHRINE', 'crcDataSourceName', 'QueryToolDemoDS', 'A')
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 10, 'U', 'SHRINE', 'shrineCellURL', 'http://localhost:9090/i2b2/services/SHRINEQEPService', 'A')
insert into hive_cell_params (ID, DATATYPE_CD, CELL_ID, PARAM_NAME_CD, VALUE, STATUS_CD) values (@i + 11, 'U', 'SHRINE', 'clientSecret', 'changeME!!!', 'A')

** CRC Database: **
There are no changes to the CRC schema
Result types must be added for SHRINE Queries.

update QT_QUERY_RESULT_TYPE set VISUAL_ATTRIBUTE_TYPE_ID = 'LH'
declare @i int
select @i = max([RESULT_TYPE_ID]) from QT_QUERY_RESULT_TYPE
insert into QT_QUERY_RESULT_TYPE(RESULT_TYPE_ID, Name, DESCRIPTION, DISPLAY_TYPE_ID, VISUAL_ATTRIBUTE_TYPE_ID, CLASSNAME) values (@i + 1, 'PATIENT_COUNT_SHRINE_XML', 'Total Number of Patients', 'CATNUM', 'LH', 'edu.harvard.i2b2.crc.dao.setfinder.QueryResultSHRINEBreakdownGenerator')
insert into QT_QUERY_RESULT_TYPE(RESULT_TYPE_ID, Name, DESCRIPTION, DISPLAY_TYPE_ID, VISUAL_ATTRIBUTE_TYPE_ID, CLASSNAME) values (@i + 2, 'PATIENT_SITE_COUNT_SHRINE_XML', 'Number of Patients by Site', 'CATNUM', 'LA', 'edu.harvard.i2b2.crc.dao.setfinder.QueryResultSHRINEBreakdownGenerator')
insert into QT_QUERY_RESULT_TYPE(RESULT_TYPE_ID, Name, DESCRIPTION, DISPLAY_TYPE_ID, VISUAL_ATTRIBUTE_TYPE_ID, CLASSNAME) values (@i + 3, 'PATIENT_AGE_COUNT_SHRINE_XML', 'Demographic Distribution by Age', 'CATNUM', 'LA', 'edu.harvard.i2b2.crc.dao.setfinder.QueryResultSHRINEBreakdownGenerator')
insert into QT_QUERY_RESULT_TYPE(RESULT_TYPE_ID, Name, DESCRIPTION, DISPLAY_TYPE_ID, VISUAL_ATTRIBUTE_TYPE_ID, CLASSNAME) values (@i + 4, 'PATIENT_SEX_COUNT_SHRINE_XML', 'Demographic Distribution by Sex', 'CATNUM', 'LA', 'edu.harvard.i2b2.crc.dao.setfinder.QueryResultSHRINEBreakdownGenerator')
insert into QT_QUERY_RESULT_TYPE(RESULT_TYPE_ID, Name, DESCRIPTION, DISPLAY_TYPE_ID, VISUAL_ATTRIBUTE_TYPE_ID, CLASSNAME) values (@i + 5, 'PATIENT_RACE_COUNT_SHRINE_XML', 'Demographic Distribution by Race', 'CATNUM', 'LA', 'edu.harvard.i2b2.crc.dao.setfinder.QueryResultSHRINEBreakdownGenerator')
insert into QT_QUERY_RESULT_TYPE(RESULT_TYPE_ID, Name, DESCRIPTION, DISPLAY_TYPE_ID, VISUAL_ATTRIBUTE_TYPE_ID, CLASSNAME) values (@i + 6, 'PATIENT_VITALSTATUS_COUNT_SHRINE_XML', 'Demographic Distribution by Vital Status', 'CATNUM', 'LA', 'edu.harvard.i2b2.crc.dao.setfinder.QueryResultSHRINEBreakdownGenerator')
insert into QT_QUERY_RESULT_TYPE(RESULT_TYPE_ID, Name, DESCRIPTION, DISPLAY_TYPE_ID, VISUAL_ATTRIBUTE_TYPE_ID, CLASSNAME) values (@i + 7, 'PATIENT_ZIP_COUNT_SHRINE_XML', 'Demographic Distribution by Zip Code', 'CATNUM', 'LA', 'edu.harvard.i2b2.crc.dao.setfinder.QueryResultSHRINEBreakdownGenerator')

** Database Schema: **

Create a Database for the SHRINE cell. The default for this is i2b2-Shrine-QEP-cell
Run the Schema.sql file
Run the Data.sql file
Update values in [dbo].[SHRINE_CONFIGURATION] to match your sites configuration.
Create a Login and User for the SHRINE cell to use. Assign this user execute persmission to all stored procedures

** SHRINE Datasource: **
Update the SHRINE-ds.xml file to reflect the user created above.

** i2b2 Node Configuration Changes **
i2b2 nodes need the zip code breakdown to be added. Run the following in the CRC database. 

  insert into [QT_BREAKDOWN_PATH] (NAME, VALUE, CREATE_DATE) values ('PATIENT_ZIP_COUNT_XML', 'select b.name_char as patient_range, count(distinct a.patient_num) as patient_count
from {{{DATABASE_NAME}}}observation_fact a, {{{DATABASE_NAME}}}concept_dimension b,
{{{DX}}} c where a.concept_cd = b.concept_cd and concept_path like ''\ACT\Demographics\ZipCode\ZIP3\%\ZIPCODE:%''
and a.patient_num = c.patient_num   group by name_char order by patient_count desc', getdate())

declare @i int
select @i = max([RESULT_TYPE_ID]) from QT_QUERY_RESULT_TYPE
insert into QT_QUERY_RESULT_TYPE(RESULT_TYPE_ID, Name, DESCRIPTION, DISPLAY_TYPE_ID, VISUAL_ATTRIBUTE_TYPE_ID, CLASSNAME) values (@i + 1, 'PATIENT_ZIP_COUNT_XML', 'Zip Code patient breakdown', 'CATNUM', 'LA', 'edu.harvard.i2b2.crc.dao.setfinder.QueryResultPatientSQLCountGenerator')
