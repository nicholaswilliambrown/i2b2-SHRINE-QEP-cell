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
	private static Log log = LogFactory.getLog(SHRINEHubPollService.class);
	protected final Logger logesapi = ESAPI.getLogger(getClass());
	private static final int pollInterval = 30000; //Polling interval in miliseconds. 
	private static int tCount = 0;
	private static Instant lastPoll = Instant.now().minusMillis(2*pollInterval);

	public void startIfNotRunning() {
		log.info("SHRINEHubPollService.startIfNotRunning");
		if (!serviceRunning())
		{
			startService();
		}

	}

	
	public void startService() {
		log.info("SHRINEHubPollService.startService");
		int itCount = tCount;
		tCount++;
		Thread one = new Thread() {
			public void run() {
				try {
					String dataSourceName = "SHRINEDemoDS";
					DataSource dataSource = ServiceLocator.getInstance().getAppServerDataSource(dataSourceName);
					Connection conn = dataSource.getConnection();
					PreparedStatement stmt = conn.prepareStatement("EXEC newQepReceivedMessage @httpStatus=?, @message=?");
					int transactionTimeout = 500;
					stmt.setQueryTimeout(transactionTimeout);
					//LogTimingUtil subLogTimingUtil = new LogTimingUtil();
					//subLogTimingUtil.setStartTime();
					
					
					String crcDataSourceName = "QueryToolDemoDS";
					DataSource crcDataSource = ServiceLocator.getInstance().getAppServerDataSource(crcDataSourceName);
					Connection crcConn = crcDataSource.getConnection();
					
					/*
					//SQL SERVER
					PreparedStatement crcStmt = crcConn.prepareStatement("update a set a.XML_VALUE = ? from QT_XML_RESULT a join QT_QUERY_RESULT_INSTANCE b on a.RESULT_INSTANCE_ID = b.RESULT_INSTANCE_ID join QT_QUERY_RESULT_TYPE c on b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and c.Name = ? join QT_QUERY_Instance d on b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID and d.QUERY_MASTER_ID = ?");					
					PreparedStatement crcStmt2 = crcConn.prepareStatement("update b set b.SET_SIZE = ?, b.REAL_SET_SIZE = ?, STATUS_TYPE_ID = e.STATUS_TYPE_ID from  QT_QUERY_RESULT_INSTANCE b join QT_QUERY_RESULT_TYPE c on b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and c.Name = ? join QT_QUERY_Instance d on b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID and d.QUERY_MASTER_ID = ? join QT_QUERY_STATUS_TYPE e on e.NAME = ?");
					*/
					
					//PostGres
					PreparedStatement crcStmt = crcConn.prepareStatement("update QT_XML_RESULT a set xml_value = ? from QT_QUERY_RESULT_INSTANCE b join QT_QUERY_RESULT_TYPE c on b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and c.Name = ? join QT_QUERY_Instance d on b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID and d.QUERY_MASTER_ID = ? where a.RESULT_INSTANCE_ID = b.RESULT_INSTANCE_ID");					
					PreparedStatement crcStmt2 = crcConn.prepareStatement("update QT_QUERY_RESULT_INSTANCE b set SET_SIZE = ?, REAL_SET_SIZE = ?, STATUS_TYPE_ID = e.STATUS_TYPE_ID from  QT_QUERY_STATUS_TYPE e	join QT_QUERY_RESULT_TYPE c on c.Name = ? join QT_QUERY_Instance d on d.QUERY_MASTER_ID = ? where e.NAME = ? and b.RESULT_TYPE_ID = c.RESULT_TYPE_ID and b.QUERY_INSTANCE_ID = d.QUERY_INSTANCE_ID");
					
					crcStmt.setQueryTimeout(transactionTimeout);
					crcStmt2.setQueryTimeout(transactionTimeout);
					/*if (csr.getSqlFinishedFlag()) {
						timeoutFlag = true;
						throw new CRCTimeOutException("The query was canceled.");
					}*/
					String deliveryAttemptID = "";

					Thread.sleep(1500);
					System.out.println("Does it work? " + itCount);
					for (int i = 0; i < 1000; i++)
					{
						lastPoll = Instant.now();
						//httpMessageResponse response = get("http://shrine-hub.shrine:8080/shrine-api/mom/receiveMessage/i2b2Plugin?timeOutSeconds=5");
						httpMessageResponse response = get("https://shrine-masscpr-dev-hub.catalyst.harvard.edu:6443/shrine-api/mom/receiveMessage/masscpri2b2qep?timeOutSeconds=5");
						//httpMessageResponse response = get("http://localhost:6060/shrine-api/mom/receiveMessage/masscpri2b2qep?timeOutSeconds=5");
						//Thread.sleep(5000);
						System.out.println("Recursive print. " + itCount + ":" + i);
						System.out.println("SHRINE POLL Response: " + response.statusCode + ":" + response.message);
						if (response.statusCode == 200)
						{
							stmt.setInt(1, response.statusCode);
							stmt.setString(2, response.message);
							ResultSet resultSet = stmt.executeQuery();
							while (resultSet.next()) {
								deliveryAttemptID = resultSet.getString("deliveryAttemptID");
								int queryID = resultSet.getInt("queryId");
								String resultType = resultSet.getString("resultType");
								int setSize = resultSet.getInt("setSize");
								String status = resultSet.getString("status");
								String x = resultSet.getString("x");
								
								
								if (!"empty".equals(deliveryAttemptID) && !"-1".equals(deliveryAttemptID))
								{
									crcStmt.setString(1, x);
									crcStmt.setString(2, resultType);
									crcStmt.setInt(3, queryID);
									
									crcStmt2.setInt(1, setSize);
									crcStmt2.setInt(2, setSize);
									crcStmt2.setString(3, resultType);
									crcStmt2.setInt(4, queryID);
									crcStmt2.setString(5, status);
							
									crcStmt.executeUpdate();
									crcStmt2.executeUpdate();
								}
							}
							log.info("\n\n\n\n\n\n\n\n\n\n\n\n\n" + deliveryAttemptID + "\n\n\n\n\n\n\n\n\n\n\n\n\n");
							
							//if (!"empty".equals(deliveryAttemptID) && !"-1".equals(deliveryAttemptID)) put("https://shrine-masscpr-dev-hub.catalyst.harvard.edu:6060/shrine-api/mom/acknowledge/" + deliveryAttemptID);
							if (!"empty".equals(deliveryAttemptID) && !"-1".equals(deliveryAttemptID)) put("https://shrine-masscpr-dev-hub.catalyst.harvard.edu:6443/shrine-api/mom/acknowledge/" + deliveryAttemptID);
							//if (!"empty".equals(deliveryAttemptID) && !"-1".equals(deliveryAttemptID)) put("http://shrine-hub.shrine:8080/shrine-api/mom/acknowledge/" + deliveryAttemptID);
						}
						//new HttpRequestMessage(HttpMethod.Put, "http://shrine-hub.shrine:8080/shrine-api/mom/acknowledge/" + messageID);
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

					System.out.println("final print." + itCount);
				} catch(Exception v) {
					System.out.println(v);
				}
			}  
		};

		one.start();

	}
	
	
	private boolean serviceRunning()
	{
		System.out.println(lastPoll);
		System.out.println(lastPoll.plusMillis(pollInterval));
		System.out.println(Instant.now());
		System.out.println(lastPoll.plusMillis(pollInterval).compareTo(Instant.now()));
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
			
			char[] passphrase = "QHrwkr3G68Mg".toCharArray();
			KeyStore ks = KeyStore.getInstance("PKCS12");
			//InputStream ksStream = new FileInputStream("D:/i2b2/wildfly-17.0.1.Final/keytool/keystore.ks");
			InputStream ksStream = new FileInputStream("/opt/wildfly-17.0.1.Final/keytool/keystore.ks");
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
			
			char[] passphrase = "QHrwkr3G68Mg".toCharArray();
			KeyStore ks = KeyStore.getInstance("PKCS12");
			//InputStream ksStream = new FileInputStream("D:/i2b2/wildfly-17.0.1.Final/keytool/keystore.ks");
			InputStream ksStream = new FileInputStream("/opt/wildfly-17.0.1.Final/keytool/keystore.ks");
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
