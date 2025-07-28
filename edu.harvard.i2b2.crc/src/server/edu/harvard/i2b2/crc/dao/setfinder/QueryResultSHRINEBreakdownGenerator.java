/*******************************************************************************
 * Copyright (c) 2006-2018 Massachusetts General Hospital 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. I2b2 is also distributed under
 * the terms of the Healthcare Disclaimer.
 ******************************************************************************/
package edu.harvard.i2b2.crc.dao.setfinder;

//D:\i2b2\code\i2b2-core-server-1.8.0.0001\edu.harvard.i2b2.crc>d:\i2b2\code\i2b2core-new-180\i2b2\edu.harvard.i2b2.data\Release_1-8\apache-ant\bin\ant.bat -f master_build.xml clean build-all deploy

/**
 * To add Length of Stay (This is for Oracle and Postgresl)  For Sql Server change the sql statement from (DX to #DX)
 * 
 * Add a entry to QT_BREAKDOWN_PATH
 *     NAME = LENGTH_OF_STAY
 *     VALUE = select length_of_stay as patient_range, count(distinct a.PATIENT_num) as patient_count  from visit_dimension a, DX b where a.patient_num = b.patient_num group by a.length_of_stay order by 1
 * 
 * Add a entry to QT_QUERY_RESULT_TYPE
 *     RESULT_TYPE_ID = 13 (Or any unused number)
 *     NAME = LENGTH_OF_STAY
 *     DESCRIPTION = Length of Dtay Brealdown
 *     DISPLAY_TYPE_ID = CATNUM
 *     VISUAL_ATTRIBUTE_TYPE_ID = LA
 *         
 * Add a new <entry> in CRCApplicationContext.xml
 * 	<entry>
 *           <key>
 *             <value>LENGTH_OF_STAY</value>
 *           </key>
 *           <value>edu.harvard.i2b2.crc.dao.setfinder.QueryResultPatientSQLCountGenerator</value>
 *         </entry>
 * 
 */


import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;

import edu.harvard.i2b2.common.exception.I2B2DAOException;
import edu.harvard.i2b2.common.util.db.JDBCUtil;
import edu.harvard.i2b2.common.util.jaxb.JAXBUtil;
import edu.harvard.i2b2.crc.dao.CRCDAO;
import edu.harvard.i2b2.crc.dao.SetFinderDAOFactory;
import edu.harvard.i2b2.crc.dao.DAOFactoryHelper;
import edu.harvard.i2b2.crc.dao.setfinder.querybuilder.ProcessTimingReportUtil;
import edu.harvard.i2b2.crc.datavo.CRCJAXBUtil;
import edu.harvard.i2b2.crc.datavo.db.QtQueryBreakdownType;
import edu.harvard.i2b2.crc.datavo.db.QtQueryResultType;
import edu.harvard.i2b2.crc.datavo.i2b2result.BodyType;
import edu.harvard.i2b2.crc.datavo.i2b2result.DataType;
import edu.harvard.i2b2.crc.datavo.i2b2result.ResultEnvelopeType;
import edu.harvard.i2b2.crc.datavo.i2b2result.ResultType;
import edu.harvard.i2b2.crc.util.LogTimingUtil;

import java.net.URL;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.*;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import edu.harvard.i2b2.common.util.ServiceLocator;
import java.sql.Connection;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import edu.harvard.i2b2.crc.datavo.db.DataSourceLookup;

/**
 * Setfinder's result genertor class. This class calculates patient break down
 * for the result type.
 * 
 * Calls the ontology to get the children for the result type and then
 * calculates the patient count for each child of the result type.
 */
public class QueryResultSHRINEBreakdownGenerator extends CRCDAO implements IResultGenerator {

	// Confinguration Values
	private static String qepDataLookup = "";
	private static String keystorePassphrase = "";
	private static String keystorePath = "";
	private static int queryWaitTime = 60;
	
	private void getConfiguration(){
		DataSource dataSource;
		try{  dataSource = ServiceLocator.getInstance().getAppServerDataSource("CRCBootStrapDS"); } catch (Exception e) { log.error("Exception locating datasource in getConfiguration: " + e); return; }
		try(
				Connection conn = dataSource.getConnection();
				PreparedStatement stmt = conn.prepareStatement("select PARAM_NAME_CD, VALUE from " + conn.getSchema() + ".hive_cell_params where status_cd <> 'D' and cell_id = 'SHRINE'");
			){
			
			ResultSet resultSet = stmt.executeQuery();
			
			String paramName = "";
			String paramValue = "";

			while (resultSet.next()) {
				paramName = resultSet.getString("PARAM_NAME_CD");
				paramValue = resultSet.getString("value");
				
				if("qepDataLookup".equals(paramName)) qepDataLookup = paramValue;
				if("keystorePassphrase".equals(paramName)) keystorePassphrase = paramValue;
				if("keystorePath".equals(paramName)) keystorePath = paramValue;
				if("queryWaitTime".equals(paramName)) queryWaitTime = Integer.parseInt(paramValue);
			}
			resultSet.close();
		}
		catch (Exception e)
		{
			log.error("Exception in getConfiguration: " + e);
		}
	}
	

	protected final Logger logesapi = ESAPI.getLogger(getClass());

	@Override
	public String getResults() {
		return xmlResult;
	}

	private String xmlResult = null;
	/**
	 * Function accepts parameter in Map. The patient count will be obfuscated
	 * if the user is OBFUS
	 */
	@Override
	public void generateResult(Map param) throws CRCTimeOutException,
	I2B2DAOException {
		
		getConfiguration();
		if ("".equals(keystorePath)) 
		{
			log.error("Exception getting configuration in QueryResultSHRINEBreakdownGenerator");
			return;
		}
		
		//System.out.println("\n\n\n\n\n\n\n QueryResultSHRINEBreakdownGenerator \n\n\n\n\n\n\n");
		SetFinderConnection sfConn = (SetFinderConnection) param
				.get("SetFinderConnection");
		SetFinderDAOFactory sfDAOFactory = (SetFinderDAOFactory) param
				.get("SetFinderDAOFactory");

		// String patientSetId = (String)param.get("PatientSetId");
		String queryInstanceId = (String) param.get("QueryInstanceId");
		String TEMP_DX_TABLE = (String) param.get("TEMP_DX_TABLE");
		String resultInstanceId = (String) param.get("ResultInstanceId");
		// String itemKey = (String) param.get("ItemKey"); test test 
		String resultTypeName = (String) param.get("ResultOptionName");
		String processTimingFlag = (String) param.get("ProcessTimingFlag");
		int obfuscatedRecordCount = (Integer) param.get("ObfuscatedRecordCount");
		int recordCount = (Integer) param.get("RecordCount");
		int transactionTimeout = (Integer) param.get("TransactionTimeout");
		boolean obfscDataRoleFlag = (Boolean)param.get("ObfuscatedRoleFlag");

		this
		.setDbSchemaName(sfDAOFactory.getDataSourceLookup()
				.getFullSchema());
		//Map ontologyKeyMap = (Map) param.get("setFinderResultOntologyKeyMap");
		String serverType = (String) param.get("ServerType");
		//		CallOntologyUtil ontologyUtil = (CallOntologyUtil) param
		//				.get("CallOntologyUtil");
		List<String> roles = (List<String>) param.get("Roles");
		String tempTableName = "";
		PreparedStatement stmt = null;
		boolean errorFlag = false, timeoutFlag = false;
		//String itemKey = "";

		int actualTotal = 0, obsfcTotal = 0;

		try {
			LogTimingUtil logTimingUtil = new LogTimingUtil();
			logTimingUtil.setStartTime();

			LogTimingUtil subLogTimingUtil = new LogTimingUtil();
			subLogTimingUtil.setStartTime();
			
			int resultInstanceIDPatientCountShrineXML = 0;
			
			DataSourceLookup dataSourceLookup = sfDAOFactory.getDataSourceLookup();
			
			System.out.println("DataSource Type: " + dataSourceLookup.getServerType());
			
			PreparedStatement riipcsxstmt = sfConn.prepareStatement("select Result_Instance_ID from QT_QUERY_RESULT_INSTANCE a join QT_QUERY_RESULT_TYPE b on a.RESULT_TYPE_ID = b.RESULT_TYPE_ID and b.name = 'PATIENT_COUNT_SHRINE_XML' and QUERY_INSTANCE_ID = " + queryInstanceId);
			riipcsxstmt.setQueryTimeout(transactionTimeout);

			// NWB - Send the final query and get the results back/
			//logesapi.debug(null,"Executing count sql [" + sqls[count] + "]");

			//
			//subLogTimingUtil.setStartTime();
			ResultSet riipcsxResultSet = riipcsxstmt.executeQuery();
			/*if (csr.getSqlFinishedFlag()) {
				timeoutFlag = true;
				throw new CRCTimeOutException("The query was canceled.");
			}*/
			
			while (riipcsxResultSet.next()) {
				resultInstanceIDPatientCountShrineXML = riipcsxResultSet.getInt("Result_Instance_ID");
			}
			riipcsxResultSet.close();
			
			if (resultInstanceIDPatientCountShrineXML > 0)
			{
				riipcsxstmt.close();
				return;
			}
			/*
			String riipcsxstmt1asql = "update QT_QUERY_RESULT_INSTANCE set STATUS_TYPE_ID = (select STATUS_TYPE_ID from QT_QUERY_STATUS_TYPE where NAME = 'PROCESSING') where QUERY_INSTANCE_ID = " + queryInstanceId;
			PreparedStatement riipcsxstmt1a = sfConn.prepareStatement(riipcsxstmt1asql);
			//riipcsxstmt3.setInt(1, queryInstanceId);
			riipcsxstmt1a.setQueryTimeout(transactionTimeout);
			riipcsxstmt1a.executeUpdate();
			riipcsxstmt1a.close();
			*/
			
			String riipcsxstmt2sql = "insert into QT_QUERY_RESULT_INSTANCE (Query_Instance_ID, RESULT_TYPE_ID, START_DATE, STATUS_TYPE_ID, DELETE_FLAG) select " + queryInstanceId + ", Result_Type_ID, now(), STATUS_TYPE_ID, 'N' from QT_QUERY_RESULT_TYPE a join QT_QUERY_STATUS_TYPE b on a.Name = 'PATIENT_COUNT_SHRINE_XML' and b.Name = 'PROCESSING'"; //Postgres 
			if (dataSourceLookup.getServerType().equalsIgnoreCase(DAOFactoryHelper.SQLSERVER)) riipcsxstmt2sql = "insert into QT_QUERY_RESULT_INSTANCE (Query_Instance_ID, RESULT_TYPE_ID, START_DATE, STATUS_TYPE_ID, DELETE_FLAG) select " + queryInstanceId + ", Result_Type_ID, getdate(), STATUS_TYPE_ID, 'N' from QT_QUERY_RESULT_TYPE a join QT_QUERY_STATUS_TYPE b on a.Name = 'PATIENT_COUNT_SHRINE_XML' and b.Name = 'PROCESSING'";
			//else if (dataSourceLookup.getServerType().equalsIgnoreCase(DAOFactoryHelper.POSTGRESQL))
			//else if (dataSourceLookup.getServerType().equalsIgnoreCase(DAOFactoryHelper.ORACLE)
			
			
			PreparedStatement riipcsxstmt2 = sfConn.prepareStatement(riipcsxstmt2sql);
			riipcsxstmt2.setQueryTimeout(transactionTimeout);
			riipcsxstmt2.executeUpdate();
			riipcsxstmt2.close();
			
			riipcsxResultSet = riipcsxstmt.executeQuery();
			/*if (csr.getSqlFinishedFlag()) {
				timeoutFlag = true;
				throw new CRCTimeOutException("The query was canceled.");
			}*/
			
			while (riipcsxResultSet.next()) {
				resultInstanceIDPatientCountShrineXML = riipcsxResultSet.getInt("Result_Instance_ID");
			}
			riipcsxResultSet.close();
			riipcsxstmt.close();



			
			//PreparedStatement riipcsxstmt3 = sfConn.prepareStatement("insert into QT_XML_RESULT (RESULT_INSTANCE_ID, XML_VALUE) values ?, '<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns10:i2b2_result_envelope xmlns:ns10=\"http://www.i2b2.org/xsd/hive/msg/result/1.1/\"><body><ns10:result name=\"PATIENT_COUNT_SHRINE_XML\" /><SHRINE /></body></ns10:i2b2_result_envelope>'");
			//PreparedStatement riipcsxstmt3 = sfConn.prepareStatement("insert into QT_XML_RESULT (RESULT_INSTANCE_ID, XML_VALUE) values (?, '')");
			
			String riipcsxstmt3sql = "insert into QT_XML_RESULT (RESULT_INSTANCE_ID, XML_VALUE) select a.RESULT_INSTANCE_ID, '<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns10:i2b2_result_envelope><body><ns10:result name=\"' || c.NAME || '\"></ns10:result></body></ns10:i2b2_result_envelope>'  From QT_QUERY_RESULT_INSTANCE a left join QT_XML_RESULT b on a.RESULT_INSTANCE_ID = b.RESULT_INSTANCE_ID join QT_QUERY_RESULT_TYPE c on a.RESULT_TYPE_ID = c.RESULT_TYPE_ID where QUERY_INSTANCE_ID = " + queryInstanceId + " and b.RESULT_INSTANCE_ID is null";
			if (dataSourceLookup.getServerType().equalsIgnoreCase(DAOFactoryHelper.SQLSERVER)) riipcsxstmt3sql = riipcsxstmt3sql.replace("||", "+");
			PreparedStatement riipcsxstmt3 = sfConn.prepareStatement(riipcsxstmt3sql);
			//riipcsxstmt3.setInt(1, queryInstanceId);
			riipcsxstmt3.setQueryTimeout(transactionTimeout);
			riipcsxstmt3.executeUpdate();
			riipcsxstmt3.close();
			
			System.out.println("Result Instance ID: " + resultInstanceIDPatientCountShrineXML);
			

			//String itemCountSql = getItemKeyFromResultType(sfDAOFactory, resultTypeName);

			//get break down count sigma from property file 

			double breakdownCountSigma = GaussianBoxMuller.getBreakdownCountSigma();
			double obfuscatedMinimumValue = GaussianBoxMuller.getObfuscatedMinimumVal();

			ResultType resultType = new ResultType();
			resultType.setName(resultTypeName);
			//stmt = sfConn.prepareStatement(itemCountSql);

			CancelStatementRunner csr = new CancelStatementRunner(stmt,
					transactionTimeout);
			Thread csrThread = new Thread(csr);
			csrThread.start();
			
			stmt = sfConn.prepareStatement("select a.query_master_id, request_xml, i2b2_request_xml from QT_QUERY_INSTANCE a join QT_QUERY_MASTER b on a.query_master_id = b.query_master_id and query_instance_id = " + queryInstanceId);
			stmt.setQueryTimeout(transactionTimeout);
			
			subLogTimingUtil.setStartTime();
			ResultSet resultSet = stmt.executeQuery();
			if (csr.getSqlFinishedFlag()) {
				timeoutFlag = true;
				throw new CRCTimeOutException("The query was canceled.");
			}
			int queryMasterId = -1;
			String requestXML = "";
			String i2b2RequestXML = "";
			
			while (resultSet.next()) {
				queryMasterId = resultSet.getInt("query_master_id");
				requestXML = resultSet.getString("request_xml");
				i2b2RequestXML = resultSet.getString("i2b2_request_xml");
			}

			csr.setSqlFinishedFlag();
			csrThread.interrupt();
			stmt.close();

			i2b2RequestXML = i2b2RequestXML.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>", "");

			httpMessageResponse response = get(qepDataLookup);			

				String dataSourceName = "SHRINEDemoDS";
				DataSource dataSource = ServiceLocator.getInstance().getAppServerDataSource(dataSourceName);
				Connection conn = dataSource.getConnection();
				PreparedStatement shrinestmt = conn.prepareStatement("EXEC [dbo].[SHRINE_CREATE_QUERY] @QueryMasterID=?, @x=?, @ix = ?");
				shrinestmt.setInt(1, queryMasterId);
				shrinestmt.setString(2, requestXML);
				shrinestmt.setString(3, i2b2RequestXML);
				//int transactionTimeout = 500;
				shrinestmt.setQueryTimeout(transactionTimeout);

				// NWB - Send the final query and get the results back/
				//logesapi.debug(null,"Executing count sql [" + sqls[count] + "]");

				//
			ResultSet shrineResultSet = null;
			
			try
			{
				subLogTimingUtil.setStartTime();
				shrineResultSet = shrinestmt.executeQuery();
				/*if (csr.getSqlFinishedFlag()) {
					timeoutFlag = true;
					throw new CRCTimeOutException("The query was canceled.");
				}*/
				int qep_query_id = 0;
				
				while (shrineResultSet.next()) {
					String hub_url = shrineResultSet.getString("hub_url");
					String content = shrineResultSet.getString("content");
					qep_query_id = shrineResultSet.getInt("qep_query_id");
					sendRequest(hub_url, content);
				}
				shrineResultSet.close();
				shrinestmt.close();
				conn.close();
			}
			catch (Exception e)
			{
				if (shrineResultSet != null) shrineResultSet.close();
				if (shrinestmt != null) shrinestmt.close();
				if (conn != null) conn.close();
				
				throw(e);
			}	
/*		
			for(int i = 0; i < 3; i++){
				DataType mdataType = new DataType();
				mdataType.setValue(String.valueOf(i * 10));
				mdataType.setColumn("column_" + i);
				mdataType.setType("int");
				resultType.getData().add(mdataType);
			}
			//Thread.sleep(10000);
*/			
			//NWB - Start Processing results
/*
			for(int i = 0; i < 50; i++)
			{
				Thread.sleep(200);
				stmt = sfConn.prepareStatement("exec [dbo].[SHRINE_POLL_RESULT_STATUS] @queryID=" + qep_query_id);
				stmt.setQueryTimeout(transactionTimeout);	
				subLogTimingUtil.setStartTime();
				resultSet = stmt.executeQuery();
				if (csr.getSqlFinishedFlag()) {
					timeoutFlag = true;
					throw new CRCTimeOutException("The query was canceled.");
				}
				String resultstatus = "EMPTY";
				while (resultSet.next()) {
					resultstatus = resultSet.getString("status");
					if("FINISHED".equals(resultstatus))
					{
						int demoCount = resultSet.getInt("count");
						subLogTimingUtil.setEndTime();
						actualTotal += demoCount;
						DataType mdataType = new DataType();

						String rangeCd = resultSet.getString("adapterNodeName");

						mdataType.setValue(String.valueOf(demoCount));
						mdataType.setColumn(rangeCd);
						mdataType.setType("int");
						resultType.getData().add(mdataType);
					}
				}

				if("FINISHED".equals(resultstatus) || "ERROR".equals(resultstatus)) break; 
			}
*/

			//Start the SHRINE listener
			postToSHRINECell();
/*
			edu.harvard.i2b2.crc.datavo.i2b2result.ObjectFactory of = new edu.harvard.i2b2.crc.datavo.i2b2result.ObjectFactory();
			BodyType bodyType = new BodyType();
			bodyType.getAny().add(of.createResult(resultType));
			ResultEnvelopeType resultEnvelop = new ResultEnvelopeType();
			resultEnvelop.setBody(bodyType);

			JAXBUtil jaxbUtil = CRCJAXBUtil.getJAXBUtil();

			StringWriter strWriter = new StringWriter();
			subLogTimingUtil.setStartTime();
			jaxbUtil.marshaller(of.createI2B2ResultEnvelope(resultEnvelop),
					strWriter);
			subLogTimingUtil.setEndTime();
			//tm.begin();
			IXmlResultDao xmlResultDao = sfDAOFactory.getXmlResultDao();
			xmlResult = strWriter.toString();
			if (resultInstanceId != null)
				xmlResultDao.createQueryXmlResult(resultInstanceId, strWriter
						.toString());
			//
			if (processTimingFlag != null) {
				if (!processTimingFlag.trim().equalsIgnoreCase(ProcessTimingReportUtil.NONE) ) {
					ProcessTimingReportUtil ptrUtil = new ProcessTimingReportUtil(sfDAOFactory.getDataSourceLookup());
					if (processTimingFlag.trim().equalsIgnoreCase(ProcessTimingReportUtil.DEBUG) ) {
						ptrUtil.logProcessTimingMessage(queryInstanceId, ptrUtil.buildProcessTiming(subLogTimingUtil, "JAXB - " + resultTypeName , ""));
					}
					logTimingUtil.setEndTime();
					ptrUtil.logProcessTimingMessage(queryInstanceId, ptrUtil.buildProcessTiming(logTimingUtil, "BUILD - " + resultTypeName , ""));
				}
			}
			//tm.commit();
*/
			/*****
			We could add polling for query completion here. That would introduce some unnecessary database transactions, but free up the thread quicker.
			*****/
			Thread.sleep(1000 * queryWaitTime); //debugging timeout		
			String riipcsxstmt1asql = "update QT_QUERY_INSTANCE set STATUS_TYPE_ID = (select STATUS_TYPE_ID from QT_QUERY_STATUS_TYPE where NAME = 'MEDIUM_QUEUE'), BATCH_MODE = 'MEDIUM_QUEUE' where QUERY_INSTANCE_ID = " + queryInstanceId;// + " and BATCH_MODE = 'RUNNING'";
			PreparedStatement riipcsxstmt1a = sfConn.prepareStatement(riipcsxstmt1asql);
			//riipcsxstmt3.setInt(1, queryInstanceId);
			riipcsxstmt1a.setQueryTimeout(transactionTimeout);
			riipcsxstmt1a.executeUpdate();
			riipcsxstmt1a.close();
			
		} catch (SQLException sqlEx) {
			// catch oracle query timeout error ORA-01013
			if (sqlEx.toString().indexOf("ORA-01013") > -1) {
				timeoutFlag = true;
				throw new CRCTimeOutException(sqlEx.getMessage(), sqlEx);
			}
			if (sqlEx.getMessage().indexOf("The query was canceled.") > -1) {
				timeoutFlag = true;
				throw new CRCTimeOutException(sqlEx.getMessage(), sqlEx);
			}
			errorFlag = true;
			log.error("Error while executing sql", sqlEx);
			throw new I2B2DAOException("Error while executing sql", sqlEx);
		} catch (Exception sqlEx) {

			errorFlag = true;
			log.error("QueryResultPatientSetGenerator.generateResult:"
					+ sqlEx.getMessage(), sqlEx);
			throw new I2B2DAOException(
					"QueryResultPatientSetGenerator.generateResult:"
							+ sqlEx.getMessage(), sqlEx);
		}
		
		
/*
		finally {

			if (resultInstanceId != null) {
				IQueryResultInstanceDao resultInstanceDao = sfDAOFactory
						.getPatientSetResultDAO();

				if (errorFlag) {
					resultInstanceDao.updatePatientSet(resultInstanceId,
							QueryStatusTypeId.STATUSTYPE_ID_ERROR, 0);
				} else {
					// set the setsize and the description of the result instance if
					// the user role is obfuscated
					if (timeoutFlag == false) { // check if the query completed
						try {
							//	tm.begin();

							String obfusMethod = "", description = null;
							if (obfscDataRoleFlag) {
								obfusMethod = IQueryResultInstanceDao.OBSUBTOTAL;
								// add () to the result type description
								// read the description from result type

							} else { 
								obfuscatedRecordCount = recordCount;
							}
							IQueryResultTypeDao resultTypeDao = sfDAOFactory.getQueryResultTypeDao();
							List<QtQueryResultType> resultTypeList = resultTypeDao
									.getQueryResultTypeByName(resultTypeName, roles);

							// add "(Obfuscated)" in the description
							//description = resultTypeList.get(0)
							//		.getDescription()
							//		+ " (Obfuscated) ";
							String queryName = sfDAOFactory.getQueryMasterDAO().getQueryDefinition(
									sfDAOFactory.getQueryInstanceDAO().getQueryInstanceByInstanceId(queryInstanceId).getQtQueryMaster().getQueryMasterId()).getName();



							resultInstanceDao.updatePatientSet(resultInstanceId,
									QueryStatusTypeId.STATUSTYPE_ID_FINISHED, null,
									//obsfcTotal, 
									obfuscatedRecordCount, recordCount, obfusMethod);

							description = resultTypeList.get(0)
									.getDescription() + " for \"" + queryName +"\"";

							// set the result instance description
							resultInstanceDao.updateResultInstanceDescription(
									resultInstanceId, description);
							//	tm.commit();
						} catch (SecurityException e) {
							throw new I2B2DAOException(
									"Failed to write obfuscated description "
											+ e.getMessage(), e);
						} catch (IllegalStateException e) {
							throw new I2B2DAOException(
									"Failed to write obfuscated description "
											+ e.getMessage(), e);
						}
					}
				}
			}
		}*/

	}


	public static void sendRequest(String apiurl, String payload){
		try{
			URL url = new URL(apiurl);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type","application/json");
			connection.setRequestProperty("Accept", "application/json");
			System.out.println("QueryResultSHRINEBreakdownGenerator sendRequest 1");
			
			char[] passphrase = keystorePassphrase.toCharArray();
			KeyStore ks = KeyStore.getInstance("PKCS12");
			InputStream ksStream = new FileInputStream(keystorePath);
			ks.load(ksStream, passphrase); // i is an InputStream reading the keystore
			ksStream.close();

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
			kmf.init(ks, passphrase);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
			tmf.init(ks);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			connection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			
			System.out.println("QueryResultSHRINEBreakdownGenerator sendRequest 2");
			
			//connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes()));
			//String payload = "{\"sampleKey\":\"sampleValue\"}";// This should be your json body i.e. {"Name" : "Mohsin"} 
			byte[] out = payload.getBytes(StandardCharsets.UTF_8);
			System.out.println("QueryResultSHRINEBreakdownGenerator sendRequest 3");
			OutputStream stream = connection.getOutputStream();
			stream.write(out);
			System.out.println("QueryResultSHRINEBreakdownGenerator sendRequest 4");
			System.out.println(connection.getResponseCode() + " " + connection.getResponseMessage()); // THis is optional
			connection.disconnect();
			System.out.println("QueryResultSHRINEBreakdownGenerator sendRequest 5");
		}catch (Exception e){
			System.out.println(e);
			System.out.println("QueryResultSHRINEBreakdownGenerator sendRequest Failed successfully");
		}
	}
	
	
		public httpMessageResponse get(String apiurl){
		httpMessageResponse response = new httpMessageResponse();
		try{
			System.out.println(apiurl);
			URL url = new URL(apiurl);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("Accept", "application/json");
			
			char[] passphrase = keystorePassphrase.toCharArray();
			KeyStore ks = KeyStore.getInstance("PKCS12");
			InputStream ksStream = new FileInputStream(keystorePath);
			ks.load(ksStream, passphrase); // i is an InputStream reading the keystore
			ksStream.close();
			
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
			kmf.init(ks, passphrase);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
			tmf.init(ks);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			connection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

			InputStream inputStream = connection.getInputStream();
			String text = new BufferedReader(
			  new InputStreamReader(inputStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n"));
			//System.out.println(text);
			response.update(connection.getResponseCode(), text);
			//System.out.println(response.statusCode + " " + response.message); // THis is optional
			connection.disconnect();
		}catch (Exception e){
			System.out.println(e);
			System.out.println("QueryResultSHRINEBreakdownGenerator get Failed successfully");
		}
		return response;
	}
	
	
	public static void postToSHRINECell(){
		try{
			String apiurl = "http://localhost:9090/i2b2/services/SHRINEQEPService/helloWorld";
			String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns6:request xmlns:ns4=\"http://www.i2b2.org/xsd/cell/crc/psm/1.1/\" xmlns:ns7=\"http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/\" xmlns:ns3=\"http://www.i2b2.org/xsd/cell/crc/pdo/1.1/\" xmlns:ns5=\"http://www.i2b2.org/xsd/hive/plugin/\" xmlns:ns2=\"http://www.i2b2.org/xsd/hive/pdo/1.1/\" xmlns:ns6=\"http://www.i2b2.org/xsd/hive/msg/1.1/\"><message_header><proxy><redirect_url>http://localhost:9090/i2b2/services/SHRINEQEPService/helloWorld</redirect_url></proxy></message_header></ns6:request>";
			
			URL url = new URL(apiurl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type","text/xml");
			connection.setRequestProperty("Accept", "text/xml");
			//connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes()));
			//String payload = "{\"sampleKey\":\"sampleValue\"}";// This should be your json body i.e. {"Name" : "Mohsin"} 
			byte[] out = payload.getBytes(StandardCharsets.UTF_8);
			OutputStream stream = connection.getOutputStream();
			stream.write(out);
			System.out.println(connection.getResponseCode() + " " + connection.getResponseMessage()); // THis is optional
			connection.disconnect();
		}catch (Exception e){
			System.out.println(e);
			System.out.println("Failed successfully");
		}
	}
/*
	private String getItemKeyFromResultType(SetFinderDAOFactory sfDAOFactory,
			String resultTypeKey) {
		//
		IQueryBreakdownTypeDao queryBreakdownTypeDao = sfDAOFactory
				.getQueryBreakdownTypeDao();
		QtQueryBreakdownType queryBreakdownType = queryBreakdownTypeDao
				.getBreakdownTypeByName(resultTypeKey);
		String itemKey = queryBreakdownType.getValue();
		return itemKey;
	}
*/

		private class httpMessageResponse
	{
		public httpMessageResponse()
		{
			statusCode  = -1;
			message = "";
		}
		
		public void update(int _statusCode, String _message)
		{
			statusCode = _statusCode;
			message = _message;
		}
		
		public int statusCode;
		public String message;
	}

}
