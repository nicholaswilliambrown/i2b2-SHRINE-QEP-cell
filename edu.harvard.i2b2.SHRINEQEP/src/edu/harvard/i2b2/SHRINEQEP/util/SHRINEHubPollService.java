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
					PreparedStatement crcStmt = crcConn.prepareStatement("EXEC SHRINE_UPDATE_RESULTS @queryId=?, @resultType=?, @x=?, @setSize=?, @status=?");
					crcStmt.setQueryTimeout(transactionTimeout);
					
					
					/*if (csr.getSqlFinishedFlag()) {
						timeoutFlag = true;
						throw new CRCTimeOutException("The query was canceled.");
					}*/
					String deliveryAttemptID = "";

				
					System.out.println("Does it work? " + itCount);
					for (int i = 0; i < 1000; i++)
					{
						lastPoll = Instant.now();
						httpMessageResponse response = get("http://shrine-hub.shrine:8080/shrine-api/mom/receiveMessage/i2b2Plugin?timeOutSeconds=5");
						//Thread.sleep(5000);
						System.out.println("Recursive print." + itCount + ":" + i);
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
							
							
							if (status != null)
							{
								crcStmt.setInt(1, queryID);
								crcStmt.setString(2, resultType);
								crcStmt.setString(3, x);
								crcStmt.setInt(4, setSize);
								crcStmt.setString(5, status);
								
								crcStmt.executeQuery();
							}
						}
						log.info("\n\n\n\n\n\n\n\n\n\n\n\n\n" + deliveryAttemptID + "\n\n\n\n\n\n\n\n\n\n\n\n\n");
						
						if (!"empty".equals(deliveryAttemptID) && !"-1".equals(deliveryAttemptID)) put("http://shrine-hub.shrine:8080/shrine-api/mom/acknowledge/" + deliveryAttemptID);
						
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
			URL url = new URL(apiurl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			//connection.setDoOutput(true);
			connection.setConnectTimeout(pollInterval * 2);
			connection.setReadTimeout(pollInterval * 2);
			//connection.setRequestProperty("Content-Type","application/json");
			connection.setRequestProperty("Accept", "application/json");
			//connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes()));
			//String payload = "{\"sampleKey\":\"sampleValue\"}";// This should be your json body i.e. {"Name" : "Mohsin"} 
			//byte[] out = payload.getBytes(StandardCharsets.UTF_8);
			//OutputStream stream = connection.getOutputStream();
			//stream.write(out);

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
			System.out.println("Failed successfully");
		}
		return response;
	}
	
		public httpMessageResponse put(String apiurl){
		httpMessageResponse response = new httpMessageResponse();
		try{
			URL url = new URL(apiurl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("PUT");
			//connection.setDoOutput(true);
			connection.setConnectTimeout(pollInterval * 2);
			connection.setReadTimeout(pollInterval * 2);
			//connection.setRequestProperty("Content-Type","application/json");
			connection.setRequestProperty("Accept", "application/json");
			//connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes()));
			//String payload = "{\"sampleKey\":\"sampleValue\"}";// This should be your json body i.e. {"Name" : "Mohsin"} 
			//byte[] out = payload.getBytes(StandardCharsets.UTF_8);
			//OutputStream stream = connection.getOutputStream();
			//stream.write(out);

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
			System.out.println("Failed successfully");
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
