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
import java.util.ArrayList;
import java.util.Map;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.Iterator;


import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;

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
import java.net.URLConnection;
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

	// Static Confinguration Values
	private static String qepDataLookup = "";
	private static String keystorePassphrase = "";
	private static String keystorePath = "";
	private static int queryWaitTime = 60;
	private static String shrineCellURL = "";
	private static String clientSecret = null;
	
	// Represents the potential statuses of a query, this is slightly more specific than in i2b2. 
	// No Response represents a message with no response. 
	// Initiated represents a query that has been sent but site data has not been returned from the hub
	// At Sites represents a query that has site data.
	// Partial result represents a query that has results from atleast one site
	// Finished is a query that has completed and has results from atleast one site
	// Error represents a query that has completed and has results from zero sites. 
	// Empty represents an empty response from the SHRINE cell, no new message received.
	private enum queryStatus { EMPTY, RECIEVED, INITIATED, AT_SITES, PARTIAL_RESULT, FINISHED, ERROR }; 
	
	
	
	
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
				if("shrineCellURL".equals(paramName)) shrineCellURL = paramValue;
				if("clientSecret".equals(paramName)) clientSecret = paramValue;
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
		
		// Get Configuration from hive_cell_params
		getConfiguration();
		if ("".equals(keystorePath)) 
		{
			log.error("Exception getting configuration in QueryResultSHRINEBreakdownGenerator");
			return;
		}
		

		// Read Parameters
		String queryInstanceId = (String) param.get("QueryInstanceId");
		String TEMP_DX_TABLE = (String) param.get("TEMP_DX_TABLE");
		String resultInstanceId = (String) param.get("ResultInstanceId");
		String resultTypeName = (String) param.get("ResultOptionName");
		String processTimingFlag = (String) param.get("ProcessTimingFlag");
		int obfuscatedRecordCount = (Integer) param.get("ObfuscatedRecordCount");
		int recordCount = (Integer) param.get("RecordCount");
		int transactionTimeout = (Integer) param.get("TransactionTimeout");
		boolean obfscDataRoleFlag = (Boolean)param.get("ObfuscatedRoleFlag");

		// Open Database connection.
		SetFinderConnection sfConn = (SetFinderConnection) param
				.get("SetFinderConnection");
		SetFinderDAOFactory sfDAOFactory = (SetFinderDAOFactory) param
				.get("SetFinderDAOFactory");
				
		this.setDbSchemaName(sfDAOFactory.getDataSourceLookup().getFullSchema());
		String serverType = (String) param.get("ServerType");
		List<String> roles = (List<String>) param.get("Roles");
		String tempTableName = "";
		PreparedStatement stmt = null;
		boolean errorFlag = false, timeoutFlag = false;


		try {
			
			// Check whether the result instance for PATIENT_COUNT_SHRINE_XML exists.
			// If it exists, the this class has already been run for this query, so we need to return. 
			// This occurs when the class is called for breakdowns, or if it is called multiple times
			// as a result of being pushed to the medium queue.
			// If the result exists, we return 
			LogTimingUtil logTimingUtil = new LogTimingUtil();
			logTimingUtil.setStartTime();

			LogTimingUtil subLogTimingUtil = new LogTimingUtil();
			subLogTimingUtil.setStartTime();
			
			
			int resultInstanceIDPatientCountShrineXML = 0;
			DataSourceLookup dataSourceLookup = sfDAOFactory.getDataSourceLookup();
			
			System.out.println("DataSource Type: " + dataSourceLookup.getServerType());
			
			PreparedStatement riipcsxstmt = sfConn.prepareStatement("select Result_Instance_ID from QT_QUERY_RESULT_INSTANCE a join QT_QUERY_RESULT_TYPE b on a.RESULT_TYPE_ID = b.RESULT_TYPE_ID and b.name = 'PATIENT_COUNT_SHRINE_XML' and QUERY_INSTANCE_ID = " + queryInstanceId);
			riipcsxstmt.setQueryTimeout(transactionTimeout);
			ResultSet riipcsxResultSet = riipcsxstmt.executeQuery();
			
			while (riipcsxResultSet.next()) {
				resultInstanceIDPatientCountShrineXML = riipcsxResultSet.getInt("Result_Instance_ID");
			}
			riipcsxResultSet.close();
			
			if (resultInstanceIDPatientCountShrineXML > 0)
			{
				riipcsxstmt.close();
				return;
			}
			
			
			
			// Create the record for the PATIENT_COUNT_SHRINE_XML result instance. 
			String riipcsxstmt2sql = "insert into QT_QUERY_RESULT_INSTANCE (Query_Instance_ID, RESULT_TYPE_ID, START_DATE, STATUS_TYPE_ID, DELETE_FLAG) select " + queryInstanceId + ", Result_Type_ID, now(), STATUS_TYPE_ID, 'N' from QT_QUERY_RESULT_TYPE a join QT_QUERY_STATUS_TYPE b on a.Name = 'PATIENT_COUNT_SHRINE_XML' and b.Name = 'PROCESSING'"; //Postgres 
			if (dataSourceLookup.getServerType().equalsIgnoreCase(DAOFactoryHelper.SQLSERVER)) riipcsxstmt2sql = "insert into QT_QUERY_RESULT_INSTANCE (Query_Instance_ID, RESULT_TYPE_ID, START_DATE, STATUS_TYPE_ID, DELETE_FLAG) select " + queryInstanceId + ", Result_Type_ID, getdate(), STATUS_TYPE_ID, 'N' from QT_QUERY_RESULT_TYPE a join QT_QUERY_STATUS_TYPE b on a.Name = 'PATIENT_COUNT_SHRINE_XML' and b.Name = 'PROCESSING'";

			PreparedStatement riipcsxstmt2 = sfConn.prepareStatement(riipcsxstmt2sql);
			riipcsxstmt2.setQueryTimeout(transactionTimeout);
			riipcsxstmt2.executeUpdate();
			riipcsxstmt2.close();
			
			
			
			// Rerun the query from above to get the result instance ID of the PATIENT_COUNT_SHRINE_XML instance
			riipcsxResultSet = riipcsxstmt.executeQuery();		
			while (riipcsxResultSet.next()) {
				resultInstanceIDPatientCountShrineXML = riipcsxResultSet.getInt("Result_Instance_ID");
			}
			riipcsxResultSet.close();
			riipcsxstmt.close();


			// Create placeholder Result XML for each result instance.
			// This is needed because the default value used by i2b2 when there is no result instance causes issues in the webclient
			String riipcsxstmt3sql = "insert into QT_XML_RESULT (RESULT_INSTANCE_ID, XML_VALUE) select a.RESULT_INSTANCE_ID, '<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns10:i2b2_result_envelope><body><ns10:result name=\"' || c.NAME || '\"></ns10:result></body></ns10:i2b2_result_envelope>'  From QT_QUERY_RESULT_INSTANCE a left join QT_XML_RESULT b on a.RESULT_INSTANCE_ID = b.RESULT_INSTANCE_ID join QT_QUERY_RESULT_TYPE c on a.RESULT_TYPE_ID = c.RESULT_TYPE_ID where QUERY_INSTANCE_ID = " + queryInstanceId + " and b.RESULT_INSTANCE_ID is null";
			if (dataSourceLookup.getServerType().equalsIgnoreCase(DAOFactoryHelper.SQLSERVER)) riipcsxstmt3sql = riipcsxstmt3sql.replace("||", "+");
			PreparedStatement riipcsxstmt3 = sfConn.prepareStatement(riipcsxstmt3sql);
			riipcsxstmt3.setQueryTimeout(transactionTimeout);
			riipcsxstmt3.executeUpdate();
			riipcsxstmt3.close();


			// Get the i2b2 request XML
			stmt = sfConn.prepareStatement("select a.query_master_id, request_xml, i2b2_request_xml from QT_QUERY_INSTANCE a join QT_QUERY_MASTER b on a.query_master_id = b.query_master_id and query_instance_id = " + queryInstanceId);
			stmt.setQueryTimeout(transactionTimeout);
			ResultSet resultSet = stmt.executeQuery();
			int queryMasterId = -1;
			String requestXML = "";
			String i2b2RequestXML = "";			
			while (resultSet.next()) {
				queryMasterId = resultSet.getInt("query_master_id");
				requestXML = resultSet.getString("request_xml");
				i2b2RequestXML = resultSet.getString("i2b2_request_xml");
			}
			stmt.close();
			// Hack to allow sql server to parse the XML.
			i2b2RequestXML = i2b2RequestXML.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>", "");
			
			// Send a get request for QEP details to the hub. For some reason the first request made to the hub often fails, so this unnecessary request hides that issue.
			httpMessageResponse response = get(qepDataLookup);			


			// Create and send the SHRINE request. 
			// Currently this is implemented in the database, it should be moved to java code
			// but this will be much easier when java is updated in future versions of i2b2.
			String dataSourceName = "SHRINEDemoDS";
			DataSource dataSource = ServiceLocator.getInstance().getAppServerDataSource(dataSourceName);
			Connection conn = dataSource.getConnection();
			PreparedStatement shrinestmt = conn.prepareStatement("EXEC [dbo].[SHRINE_CREATE_QUERY] @QueryMasterID=?, @x=?, @ix = ?");
			shrinestmt.setInt(1, queryMasterId);
			shrinestmt.setString(2, requestXML);
			shrinestmt.setString(3, i2b2RequestXML);
			//int transactionTimeout = 500;
			shrinestmt.setQueryTimeout(transactionTimeout);
			ResultSet shrineResultSet = null;		
			int qep_query_id = -1;
			try
			{
				subLogTimingUtil.setStartTime();
				shrineResultSet = shrinestmt.executeQuery();
				/*if (csr.getSqlFinishedFlag()) {
					timeoutFlag = true;
					throw new CRCTimeOutException("The query was canceled.");
				}*/
				
				
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
			
			

			//Start the SHRINE listener
			//postToSHRINECell();
			//pollSHRINECellForUpdates(qep_query_id, 0, clientSecret);
			int previous_message_id = 0;
			int maxPolls = 120;
			for (int i = 0; i < maxPolls; i++)
			{
				ResultInstanceUpdate messageResults = getUpdateFromSHRINECell(qep_query_id, previous_message_id, clientSecret, log);
				if (messageResults.messageID > 0) previous_message_id = messageResults.messageID;
				for(ResultInstanceUpdateResult r: messageResults.results)
				{
					if (r.status.compareTo(queryStatus.AT_SITES) >= 0)
					{
						i = maxPolls;
					}
					
				}
			}
			/*****
			We could add polling for query completion here. That would introduce some unnecessary database transactions, but free up the thread quicker.
			*****/

			
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

	public static HttpURLConnection getConnection(String apiurl, String requestMethod)
	{
		if(apiurl.startsWith("https://"))
		{
			return getHttpsConnection(apiurl, requestMethod);
		}
		else
		{
			return getHttpConnection(apiurl, requestMethod);
		}
	}
	
	public static HttpURLConnection getHttpsConnection(String apiurl, String requestMethod)
	{
		HttpsURLConnection connection = null;
		try(InputStream ksStream = new FileInputStream(keystorePath);)
		{
			URL url = new URL(apiurl);
			connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod(requestMethod);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type","application/json");
			connection.setRequestProperty("Accept", "application/json");
			
			char[] passphrase = keystorePassphrase.toCharArray();
			KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(ksStream, passphrase); // i is an InputStream reading the keystore
			ksStream.close();

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
			kmf.init(ks, passphrase);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
			tmf.init(ks);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			connection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			
		}catch (Exception e){
			System.out.println(e);
			System.out.println("Failed to create HTTPS connection");
		}
		return connection;
	}
	
		public static HttpURLConnection getHttpConnection(String apiurl, String requestMethod)
	{
		try(InputStream ksStream = new FileInputStream(keystorePath);)
		{
			URL url = new URL(apiurl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(requestMethod);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type","application/json");
			connection.setRequestProperty("Accept", "application/json");
			
			return connection;
		}catch (Exception e){
			System.out.println(e);
			System.out.println("Failed to create HTTPS connection");
			return null;
		}
	}


	public static void sendRequest(String apiurl, String payload){
		try{
			HttpURLConnection connection = getConnection(apiurl, "PUT");
			
			//connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes()));
			//String payload = "{\"sampleKey\":\"sampleValue\"}";// This should be your json body i.e. {"Name" : "Mohsin"} 
			byte[] out = payload.getBytes(StandardCharsets.UTF_8);
			OutputStream stream = connection.getOutputStream();
			stream.write(out);
			System.out.println(connection.getResponseCode() + " " + connection.getResponseMessage()); // THis is optional
			connection.disconnect();
		}catch (Exception e){
			System.out.println(e);
			System.out.println("QueryResultSHRINEBreakdownGenerator sendRequest Failed");
		}
	}
	
	
		public httpMessageResponse get(String apiurl){
		httpMessageResponse response = new httpMessageResponse();
		try{
			HttpURLConnection connection = getConnection(apiurl, "GET");

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
			System.out.println("QueryResultSHRINEBreakdownGenerator get Failed");
		}
		return response;
	}
	
	/****************************************
	*                                       *
	* HTTP versions of SHRINE communication *
	* uncomment for testing use only        *
	*                                       *
	****************************************/
/*	public static void sendRequest(String apiurl, String payload){
		try{
			URL url = new URL(apiurl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type","application/json");
			connection.setRequestProperty("Accept", "application/json");
			System.out.println("QueryResultSHRINEBreakdownGenerator sendRequest 1");
			
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
			System.out.println("QueryResultSHRINEBreakdownGenerator sendRequest Failed successfully, URL: " + apiurl + " payload: " + payload);
		}
	}
	
	
		public httpMessageResponse get(String apiurl){
		httpMessageResponse response = new httpMessageResponse();
		try{
			System.out.println(apiurl);
			URL url = new URL(apiurl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("Accept", "application/json");

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
*/

	
	public static void postToSHRINECell(){
		try{
			String apiurl = shrineCellURL + "/startListener";
			String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns6:request xmlns:ns4=\"http://www.i2b2.org/xsd/cell/crc/psm/1.1/\" xmlns:ns7=\"http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/\" xmlns:ns3=\"http://www.i2b2.org/xsd/cell/crc/pdo/1.1/\" xmlns:ns5=\"http://www.i2b2.org/xsd/hive/plugin/\" xmlns:ns2=\"http://www.i2b2.org/xsd/hive/pdo/1.1/\" xmlns:ns6=\"http://www.i2b2.org/xsd/hive/msg/1.1/\"><message_header><proxy><redirect_url>http://localhost:9090/i2b2/services/SHRINEQEPService/startListener</redirect_url></proxy></message_header></ns6:request>";
			
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
			//System.out.println(connection.getResponseCode() + " " + connection.getResponseMessage()); // THis is optional
			connection.disconnect();
		}catch (Exception e){
			System.out.println(e);
			System.out.println("Failed successfully");
		}
	}

	private static ResultInstanceUpdate getUpdateFromSHRINECell(int queryID, int previousMessageID, String _clientSecret, Log _log)
	{
		ResultInstanceUpdate riu = new ResultInstanceUpdate();
		List<ResultInstanceUpdateResult> results = new ArrayList<>();
		httpMessageResponse response = pollSHRINECellForUpdates(queryID, previousMessageID, _clientSecret);
		if (response.statusCode != 200) return riu;
		
		OMElement x;
		try
		{
			x = AXIOMUtil.stringToOM(response.message);
			Iterator<OMElement> it = x.getChildElements();
			while (it.hasNext())
			{
				OMElement next = it.next();
				if("message_body".equals(next.getLocalName()))
				{
					Iterator<OMElement> it1 = next.getChildElements();
					while (it1.hasNext())
					{
						OMElement next1 = it1.next();
						if("query_id".equals(next1.getLocalName())) riu.queryID = Integer.parseInt(next1.getText());
						if("message_id".equals(next1.getLocalName())) riu.messageID = Integer.parseInt(next1.getText());
						if("results".equals(next1.getLocalName())) 
						{
							Iterator<OMElement> it2 = next1.getChildElements();
							while (it2.hasNext())
							{
								OMElement next2 = it2.next();
								
								if("result".equals(next2.getLocalName()))
								{
									
									String result_type = "";
									int set_size = -1;
									String status = "";
									String xml_value = "";
									Iterator<OMElement> it3 = next2.getChildElements();
									while (it3.hasNext())
									{
										OMElement next3 = it3.next();
										if("result_type".equals(next3.getLocalName())) result_type = next3.getText();
										if("set_size".equals(next3.getLocalName())) set_size = Integer.parseInt(next3.getText());
										if("status".equals(next3.getLocalName())) status = (next3.getText());
										if("xml_value".equals(next3.getLocalName())) xml_value = (next3.getText());
									}
									results.add(new ResultInstanceUpdateResult(result_type, set_size, status, xml_value));
								}
								
							}
						}
						//_log.error("getUpdateFromSHRINECell received message: " + queryID + "  :  " +  previousMessageID);
					}
				}
				
			}
			
			
		}
		catch (Exception e)
		{
			 _log.error("Exception converting responce to OMElement  in QueryResultSHRINEBreakdownGenerator.getUpdateFromSHRINECell: " + e + "  :  " + response.message); return riu; 
		}
		riu.results = results;
		return riu;
	}

	public static httpMessageResponse pollSHRINECellForUpdates(int queryID, int previousMessageID, String _clientSecret){
		httpMessageResponse response = new httpMessageResponse();
		try{
			String apiurl = shrineCellURL + "/getCRCMessage";
			String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns6:request xmlns:ns4=\"http://www.i2b2.org/xsd/cell/crc/psm/1.1/\" xmlns:ns7=\"http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/\" xmlns:ns3=\"http://www.i2b2.org/xsd/cell/crc/pdo/1.1/\" xmlns:ns5=\"http://www.i2b2.org/xsd/hive/plugin/\" xmlns:ns2=\"http://www.i2b2.org/xsd/hive/pdo/1.1/\" xmlns:ns6=\"http://www.i2b2.org/xsd/hive/msg/1.1/\"><message_header><proxy><redirect_url>http://localhost:9090/i2b2/services/SHRINEQEPService/startListener</redirect_url></proxy></message_header><message_body><query_id>" + queryID + "</query_id><previous_message_id>" + previousMessageID + "</previous_message_id><client_secret>" + _clientSecret + "</client_secret></message_body></ns6:request>";
			
			System.out.println(payload);
			
			
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
			
			InputStream inputStream = connection.getInputStream();
			String text = new BufferedReader(
			  new InputStreamReader(inputStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n"));
			response.update(connection.getResponseCode(), text);
			connection.disconnect();
			
			//System.out.println("pollSHRINECellForUpdates" + text);
			
			//OMElement x = AXIOMUtil.stringToOM(
		}catch (Exception e){
			System.out.println(e);
			System.out.println("Failed successfully");
		}
		return response;
	}

	private static class httpMessageResponse
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
	
	private static class ResultInstanceUpdate
	{
		public List<ResultInstanceUpdateResult> results;
		public int queryID = -1;
		public int messageID = -1;
	}		
	
	private static class ResultInstanceUpdateResult
	{
		public String result_type;
		public int set_size;
		public queryStatus status = queryStatus.EMPTY;
		public String xml_value;
		
		public ResultInstanceUpdateResult(String _result_type, int _set_size, String _status, String _xml_value)
		{
			result_type = _result_type;
			set_size = _set_size;
			xml_value = _xml_value.replace("&gt;", ">").replace("&lt;", "<").replace("&amp;", "&");
			if (_status.equals("PROCESSING"))
			{
				if (xml_value.contains("site name=\"")) status = queryStatus.AT_SITES;
				else status = queryStatus.INITIATED;
			}
			else if (_status.equals("FINISHED")) status = queryStatus.FINISHED;
			else if (_status.equals("ERROR")) status = queryStatus.ERROR;
			else  status = queryStatus.EMPTY;
		}
	}

}
