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

public class SHRINEHubPollService {
	private static Log log = LogFactory.getLog(SHRINEHubPollService.class);
	protected final Logger logesapi = ESAPI.getLogger(getClass());
	private static final int pollInterval = 5000; //Polling interval in miliseconds. 
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
					System.out.println("Does it work? " + itCount);
					for (int i = 0; i < 30; i++)
					{
						lastPoll = Instant.now();
						sendRequest("http://shrine-hub.shrine:8080/shrine-api/mom/receiveMessage/i2b2Plugin?timeOutSeconds=5");
						//Thread.sleep(5000);
						System.out.println("Recursive print." + itCount);
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

	public static void sendRequest(String apiurl){
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
			System.out.println(connection.getResponseCode() + " " + connection.getResponseMessage()); // THis is optional
			InputStream inputStream = connection.getInputStream();
			String text = new BufferedReader(
			  new InputStreamReader(inputStream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n"));
			System.out.println(text);
			connection.disconnect();
		}catch (Exception e){
			System.out.println(e);
			System.out.println("Failed successfully");
		}
	}
}
