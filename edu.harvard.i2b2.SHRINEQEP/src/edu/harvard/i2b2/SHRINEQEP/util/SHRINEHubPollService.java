package edu.harvard.i2b2.SHRINEQEP.util;

import edu.harvard.i2b2.common.exception.I2B2Exception;

import org.apache.axiom.om.OMElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;

import java.time.Instant;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.io.*;
import java.util.stream.Collectors;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;

import javax.sql.DataSource;
import edu.harvard.i2b2.common.util.ServiceLocator;
//import edu.harvard.i2b2.crc.util.LogTimingUtil;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.HttpsURLConnection;

public class SHRINEHubPollService {
	
	/** 
	* Set these values in the Hive database:
	
		delete from hive_cell_params where CELL_ID = 'SHRINE'

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
	**/
	
	// Configuration Values loaded from Hive database.
	private static String keystorePath = "";
	private static String qepQueueName = "";
	private static String crcDatabaseType = "";
	private static String qepDataLookup = "";
	private static String keystorePassphrase = "";
	private static String hubURL = "";
	private static String dataSourceName = "";
	private static String crcDataSourceName = "";
	
	private static Log log = LogFactory.getLog(SHRINEHubPollService.class);
	protected final Logger logesapi = ESAPI.getLogger(getClass());
	private static final int pollInterval = 30000; //Polling interval in miliseconds. 
	private static int tCount = 0;
	private static Instant lastPoll = Instant.now().minusMillis(2*pollInterval);
	
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
				if("crcDatabaseType".equals(paramName)) crcDatabaseType = paramValue;
				if("hubURL".equals(paramName)) hubURL = paramValue;
				if("qepQueueName".equals(paramName)) qepQueueName = paramValue;
				if("dataSourceName".equals(paramName)) dataSourceName = paramValue;
				if("crcDataSourceName".equals(paramName)) crcDataSourceName = paramValue;
			}
			resultSet.close();
		}
		catch (Exception e)
		{
			log.error("Exception in getConfiguration: " + e);
		}
	}

	
	public void startIfNotRunning() {
		log.info("SHRINEHubPollService.startIfNotRunning");
		if (!serviceRunning())
		{
			startService();
		}

	}

	
	public void startService() {
		log.info("SHRINEHubPollService.startService");
		
		getConfiguration();
		if ("".equals(keystorePath)) 
		{
			log.error("Exception getting configuration in SHRINEHubPollService.startService");
			return;
		}
		
		int itCount = tCount;
		tCount++;
		Thread one = new Thread() {
			public void run() {
				DataSource dataSource;
				DataSource crcDataSource;
				try	{  
					dataSource = ServiceLocator.getInstance().getAppServerDataSource(dataSourceName);
					crcDataSource = ServiceLocator.getInstance().getAppServerDataSource(crcDataSourceName);
				} catch (Exception e) { log.error("Exception locating datasources in SHRINEHubPollService.startService: " + e); return; }
				try {
					
					Connection conn = dataSource.getConnection();
					PreparedStatement stmt = conn.prepareStatement("EXEC newQepReceivedMessage @httpStatus=?, @message=?");
					int transactionTimeout = 500;
					stmt.setQueryTimeout(transactionTimeout);
					//LogTimingUtil subLogTimingUtil = new LogTimingUtil();
					//subLogTimingUtil.setStartTime();
					
					
					
					
					Connection crcConn = crcDataSource.getConnection();


					String crcStmtSql = "select RESULT_INSTANCE_ID from QT_QUERY_RESULT_INSTANCE a join QT_QUERY_INSTANCE b on a.QUERY_INSTANCE_ID = b.QUERY_INSTANCE_ID and QUERY_MASTER_ID = ? join QT_QUERY_RESULT_TYPE c on a.RESULT_TYPE_ID = c.RESULT_TYPE_ID and c.NAME = ?"; // Get Result Instance ID
					String crcStmt2Sql = ""; // Update QT_QUERY_RESULT_INSTANCE
					String crcStmt3Sql = ""; // Insert into QT_QUERY_RESULT_INSTANCE if non existant (not yet implemented)
					String crcStmt4Sql = "select XML_RESULT_ID from QT_XML_RESULT where Result_instance_ID = ?"; // Get XML Result ID
					String crcStmt5Sql = "update QT_XML_RESULT set XML_VALUE = ? where XML_RESULT_ID = ?"; // Update QT_XML_RESULT if record already exists
					String crcStmt6Sql = "insert into QT_XML_RESULT(Result_Instance_ID, XML_Value) values (?, ?)"; // Insert into QT_XML_RESULT if no record 
					//String crcStmt7Sql = "update QT_QUERY_INSTANCE set BATCH_MODE = 'FINISHED', STATUS_TYPE_ID = (select STATUS_TYPE_ID from QT_QUERY_STATUS_TYPE where NAME = 'FINISHED') where QUERY_MASTER_ID = ?"; // 
					String crcStmt7Sql = "update QT_QUERY_INSTANCE set BATCH_MODE = ?, STATUS_TYPE_ID = (select STATUS_TYPE_ID from QT_QUERY_STATUS_TYPE where NAME = ?) where QUERY_MASTER_ID = ?"; // 
					
					if ("SQLSERVER".equals(crcDatabaseType))
					{
						//crcStmt5Sql = "update a set a.XML_VALUE = ? from QT_XML_RESULT a join QT_QUERY_RESULT_INSTANCE b on a.RESULT_INSTANCE_ID = b.RESULT_INSTANCE_ID join QT_QUERY_RESULT_TYPE c on b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and c.Name = ? join QT_QUERY_Instance d on b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID and d.QUERY_MASTER_ID = ?";
						crcStmt2Sql ="update b set b.SET_SIZE = ?, b.REAL_SET_SIZE = ?, STATUS_TYPE_ID = e.STATUS_TYPE_ID from  QT_QUERY_RESULT_INSTANCE b join QT_QUERY_RESULT_TYPE c on b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and c.Name = ? join QT_QUERY_Instance d on b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID and d.QUERY_MASTER_ID = ? join QT_QUERY_STATUS_TYPE e on e.NAME = ?";
						crcStmt3Sql = "insert into QT_QUERY_RESULT_INSTANCE (Query_Instance_ID, RESULT_TYPE_ID, START_DATE, STATUS_TYPE_ID, DELETE_FLAG) select query_instance_id, Result_Type_ID, getDate(), STATUS_TYPE_ID, 'N' from QT_QUERY_RESULT_TYPE a join QT_QUERY_STATUS_TYPE b on a.Name = ? and b.Name = 'PROCESSING' join QT_QUERY_INSTANCE c on c.Query_master_ID = ?";
					}
					else if ("POSTGRES".equals(crcDatabaseType))
					{
						//crcStmt5Sql = "update QT_XML_RESULT a set xml_value = ? from QT_QUERY_RESULT_INSTANCE b join QT_QUERY_RESULT_TYPE c on b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and c.Name = ? join QT_QUERY_Instance d on b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID and d.QUERY_MASTER_ID = ? where a.RESULT_INSTANCE_ID = b.RESULT_INSTANCE_ID";
						crcStmt2Sql = "update QT_QUERY_RESULT_INSTANCE b set SET_SIZE = ?, REAL_SET_SIZE = ?, STATUS_TYPE_ID = e.STATUS_TYPE_ID from  QT_QUERY_STATUS_TYPE e	join QT_QUERY_RESULT_TYPE c on c.Name = ? join QT_QUERY_Instance d on d.QUERY_MASTER_ID = ? where e.NAME = ? and b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID";
						crcStmt3Sql = "insert into QT_QUERY_RESULT_INSTANCE (Query_Instance_ID, RESULT_TYPE_ID, START_DATE, STATUS_TYPE_ID, DELETE_FLAG) select c.query_instance_id, a.Result_Type_ID, now(), b.STATUS_TYPE_ID, 'N' from QT_QUERY_RESULT_TYPE a join QT_QUERY_STATUS_TYPE b on a.Name = ? and b.Name = 'PROCESSING' join QT_QUERY_INSTANCE c on c.Query_master_ID = ?";
					}
					/*
					//SQL SERVER
					PreparedStatement crcStmt = crcConn.prepareStatement("update a set a.XML_VALUE = ? from QT_XML_RESULT a join QT_QUERY_RESULT_INSTANCE b on a.RESULT_INSTANCE_ID = b.RESULT_INSTANCE_ID join QT_QUERY_RESULT_TYPE c on b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and c.Name = ? join QT_QUERY_Instance d on b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID and d.QUERY_MASTER_ID = ?");					
					PreparedStatement crcStmt2 = crcConn.prepareStatement("update b set b.SET_SIZE = ?, b.REAL_SET_SIZE = ?, STATUS_TYPE_ID = e.STATUS_TYPE_ID from  QT_QUERY_RESULT_INSTANCE b join QT_QUERY_RESULT_TYPE c on b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and c.Name = ? join QT_QUERY_Instance d on b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID and d.QUERY_MASTER_ID = ? join QT_QUERY_STATUS_TYPE e on e.NAME = ?");
					*/
					/*
					//PostGres
					PreparedStatement crcStmt = crcConn.prepareStatement("update QT_XML_RESULT a set xml_value = ? from QT_QUERY_RESULT_INSTANCE b join QT_QUERY_RESULT_TYPE c on b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and c.Name = ? join QT_QUERY_Instance d on b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID and d.QUERY_MASTER_ID = ? where a.RESULT_INSTANCE_ID = b.RESULT_INSTANCE_ID");					
					PreparedStatement crcStmt2 = crcConn.prepareStatement("update QT_QUERY_RESULT_INSTANCE b set SET_SIZE = ?, REAL_SET_SIZE = ?, STATUS_TYPE_ID = e.STATUS_TYPE_ID from  QT_QUERY_STATUS_TYPE e	join QT_QUERY_RESULT_TYPE c on c.Name = ? join QT_QUERY_Instance d on d.QUERY_MASTER_ID = ? where e.NAME = ? and b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID");
					*/
					
					PreparedStatement crcStmt = crcConn.prepareStatement(crcStmtSql);
					PreparedStatement crcStmt2 = crcConn.prepareStatement(crcStmt2Sql);
					PreparedStatement crcStmt3 = crcConn.prepareStatement(crcStmt3Sql);
					PreparedStatement crcStmt4 = crcConn.prepareStatement(crcStmt4Sql);
					PreparedStatement crcStmt5 = crcConn.prepareStatement(crcStmt5Sql);
					PreparedStatement crcStmt6 = crcConn.prepareStatement(crcStmt6Sql);
					PreparedStatement crcStmt7 = crcConn.prepareStatement(crcStmt7Sql);
					
					
					crcStmt.setQueryTimeout(transactionTimeout);
					crcStmt2.setQueryTimeout(transactionTimeout);
					crcStmt3.setQueryTimeout(transactionTimeout);
					crcStmt4.setQueryTimeout(transactionTimeout);
					crcStmt5.setQueryTimeout(transactionTimeout);
					crcStmt6.setQueryTimeout(transactionTimeout);
					crcStmt7.setQueryTimeout(transactionTimeout);
					/*if (csr.getSqlFinishedFlag()) {
						timeoutFlag = true;
						throw new CRCTimeOutException("The query was canceled.");
					}*/
					String deliveryAttemptID = "";

					Thread.sleep(1500);
					//System.out.println("Does it work? " + itCount);
					try{
						//for (int i = 0; i < 50; i++)
						while(true)
						{
							lastPoll = Instant.now();
							httpMessageResponse response = get(hubURL + "/mom/receiveMessage/" + qepQueueName + "?timeOutSeconds=5");
							//Thread.sleep(5000);
							//System.out.println("Recursive print. " + itCount + ":" + i);
							System.out.println("SHRINE POLL Response: " + response.statusCode + ":" + response.message);
							if (response.statusCode == 200)
							{
								stmt.setInt(1, response.statusCode);
								stmt.setString(2, response.message);
								ResultSet resultSet = stmt.executeQuery();
								boolean firstLoop = true;
								while (resultSet.next()) {
									deliveryAttemptID = resultSet.getString("deliveryAttemptID");
									int queryID = resultSet.getInt("queryId");
									String resultType = resultSet.getString("resultType");
									int setSize = resultSet.getInt("setSize");
									String status = resultSet.getString("status");
									String x = resultSet.getString("x");
									
									try{
										if (!"empty".equals(deliveryAttemptID) && !"-1".equals(deliveryAttemptID))
										{
											crcStmt.setInt(1, queryID);
											crcStmt.setString(2, resultType);
											int resultInstanceID = 0;
											ResultSet crcResultSet = crcStmt.executeQuery();
											while (crcResultSet.next()) {
												resultInstanceID = crcResultSet.getInt("RESULT_INSTANCE_ID");
											}
											crcResultSet.close();
											if (resultInstanceID == 0){
												crcStmt3.setInt(2, queryID);
												crcStmt3.setString(1, resultType);
												crcStmt3.executeUpdate();
												
												crcResultSet = crcStmt.executeQuery();
												while (crcResultSet.next()) {
													resultInstanceID = crcResultSet.getInt("RESULT_INSTANCE_ID");
												}
												crcResultSet.close();
											}
											
											crcStmt2.setInt(1, setSize);
											crcStmt2.setInt(2, setSize);
											crcStmt2.setString(3, resultType);
											crcStmt2.setInt(4, queryID);
											crcStmt2.setString(5, status);
											crcStmt2.executeUpdate();
											
											crcStmt4.setInt(1, resultInstanceID);
											log.info("resultInstanceID: " + resultInstanceID);
											int xmlResultID = 0;
											crcResultSet = crcStmt4.executeQuery();
											while (crcResultSet.next()) {
												xmlResultID = crcResultSet.getInt("XML_RESULT_ID");
											}
											crcResultSet.close();
											log.info("xmlResultID: " + xmlResultID);
											if (xmlResultID > 0){
												crcStmt5.setString(1, x);
												crcStmt5.setInt(2, xmlResultID);
												crcStmt5.executeUpdate();
											}
											else {
												crcStmt6.setInt(1, resultInstanceID);
												crcStmt6.setString(2, x);
												crcStmt6.executeUpdate();
											}
											
											if (firstLoop)
											{
												firstLoop = false;
												if ("PROCESSING".equals(status))
												{
													crcStmt7.setString(1, "RUNNING");
													crcStmt7.setString(2, "PROCESSING");
													crcStmt7.setInt(3, queryID);
													//crcStmt7.setString(2, x);
													crcStmt7.executeUpdate();
												}
												if ("FINISHED".equals(status))
												{
													crcStmt7.setString(1, "FINISHED");
													crcStmt7.setString(2, "FINISHED");
													crcStmt7.setInt(3, queryID);
													//crcStmt7.setString(2, x);
													crcStmt7.executeUpdate();
												}
												if ("ERROR".equals(status))
												{
													crcStmt7.setString(1, "ERROR");
													crcStmt7.setString(2, "ERROR");
													crcStmt7.setInt(3, queryID);
													//crcStmt7.setString(2, x);
													crcStmt7.executeUpdate();
												}
											}
											firstLoop = false;
											
											//stmt.close();
											//crcStmt.close();
											//crcStmt2.close();
											//crcStmt3.close();
											//crcStmt4.close();
											//crcStmt5.close();
											//crcStmt6.close();
											//crcStmt7.close();
										}
									}
									catch(Exception v) {
										System.out.println(v);
									}
								}
								resultSet.close();
								log.info("\nSHRINE DELIVERY ATTEMPT ID: " + deliveryAttemptID + "\n");
								
								if (!"empty".equals(deliveryAttemptID) && !"-1".equals(deliveryAttemptID)) put(hubURL + "/mom/acknowledge/" + deliveryAttemptID);
							}
	/*						


							PreparedStatement stmt = null;
							Connection conn = = new Connection 
							stmt = sfConn.prepareStatement("exec [dbo].[SHRINE_CREATE_QUERY] @QueryInstanceID=" + queryInstanceId);
							stmt.setQueryTimeout(transactionTimeout);
							//logesapi.debug(null,"Executing count sql [" + sqls[count] + "]");

							//
							subLogTimingUtil.setStartTime();
							ResultSet resultSet = stmt.executeQuery();
							if (csr.getSqlFinishedFlag()) {
								timeoutFlag = true;
								throw new CRCTimeOutException("The query was canceled.");
							}
							int qep_query_id = 0;
							
							while (resultSet.next()) {
								String hub_url = resultSet.getString("hub_url");
								String content = resultSet.getString("content");
								qep_query_id = resultSet.getInt("qep_query_id");
								sendRequest(hub_url, content);
							}
	*/
						}
					}
					catch(Exception v) {
						stmt.close();
						crcStmt.close();
						crcStmt2.close();
						crcStmt3.close();
						crcStmt4.close();
						crcStmt5.close();
						crcStmt6.close();
						crcStmt7.close();
						conn.close();
						crcConn.close();
						System.out.println(v);
					}
					//System.out.println("final print." + itCount);

					
				} catch(Exception v) {
					System.out.println(v);
				}
			}  
		};

		one.start();

	}
	
	
	private boolean serviceRunning()
	{
		return lastPoll.plusMillis(pollInterval).compareTo(Instant.now()) >= 0;
	}


	public httpMessageResponse get(String apiurl){
		httpMessageResponse response = new httpMessageResponse();
		try{
			System.out.println(apiurl);
			URL url = new URL(apiurl);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(pollInterval * 2);
			connection.setReadTimeout(pollInterval * 2);
			connection.setRequestProperty("Accept", "application/json");
			
			char[] passphrase = keystorePassphrase.toCharArray();
			KeyStore ks = KeyStore.getInstance("PKCS12");
			//InputStream ksStream = new FileInputStream("D:/i2b2/wildfly-17.0.1.Final/keytool/keystore.ks");
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
			System.out.println("SHRINE HUB POLL SERVICE get Failed successfully");
		}
		return response;
	}
	
	public httpMessageResponse put(String apiurl){
		httpMessageResponse response = new httpMessageResponse();
		try{
			URL url = new URL(apiurl);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("PUT");
			connection.setConnectTimeout(pollInterval * 2);
			connection.setReadTimeout(pollInterval * 2);
			connection.setRequestProperty("Accept", "application/json");
			
			char[] passphrase = keystorePassphrase.toCharArray();
			KeyStore ks = KeyStore.getInstance("PKCS12");
			//InputStream ksStream = new FileInputStream("D:/i2b2/wildfly-17.0.1.Final/keytool/keystore.ks");
			InputStream ksStream = new FileInputStream(keystorePath);
			ks.load(ksStream, passphrase); // i is an InputStream reading the keystore

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
			System.out.println("SHRINE HUB POLL SERVICE put Failed successfully");
		}
		return response;
	}
	
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
