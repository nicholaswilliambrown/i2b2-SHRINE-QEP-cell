/*******************************************************************************
 * Copyright (c) 2006-2018 Massachusetts General Hospital 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. I2b2 is also distributed under
 * the terms of the Healthcare Disclaimer.
 ******************************************************************************/
/*

 * 
 * Contributors:
 * 		Raj Kuttan
 * 		Lori Phillips
 */
package edu.harvard.i2b2.SHRINEQEP.ws;

import edu.harvard.i2b2.common.exception.I2B2Exception;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import edu.harvard.i2b2.SHRINEQEP.util.SHRINEHubPollService;
import java.util.Iterator;
import java.time.LocalTime;
//import javax.xml.stream.XMLStreamException;


/**
 * This is webservice skeleton class. It parses incoming Workplace service requests
 * and  generates responses in the Work Data Object XML format.
 *
 */
public class SHRINEQEPService {
	private static Log log = LogFactory.getLog(SHRINEQEPService.class);
	protected final Logger logesapi = ESAPI.getLogger(getClass());
	private static int tCount = 0;
	private static SHRINEHubPollService poller = new SHRINEHubPollService();

	/**
	 * This function is main webservice interface to get vocab data
	 * for a query. It uses AXIOM elements(OMElement) to conveniently parse
	 * xml messages.
	 *
	 * It excepts incoming request in i2b2 message format, which wraps a Workplace
	 * query inside a vocab query request object. The response is also will be in i2b2
	 * message format, which will wrap work data object. Work data object will
	 * have all the results returned by the query.
	 *
	 *
	 * @param getChildren
	 * @return OMElement in i2b2message format
	 * @throws Exception
	 */
	public OMElement startListener(OMElement helloElement) 
			throws I2B2Exception {
		poller.startIfNotRunning();
		return helloElement;

	}




	/*
	curl.exe -d "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns6:request xmlns:ns4=\"http://www.i2b2.org/xsd/cell/crc/psm/1.1/\" xmlns:ns7=\"http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/\" xmlns:ns3=\"http://www.i2b2.org/xsd/cell/crc/pdo/1.1/\" xmlns:ns5=\"http://www.i2b2.org/xsd/hive/plugin/\" xmlns:ns2=\"http://www.i2b2.org/xsd/hive/pdo/1.1/\" xmlns:ns6=\"http://www.i2b2.org/xsd/hive/msg/1.1/\"><message_header><proxy><redirect_url>http://localhost:9090/i2b2/services/SHRINEQEPService/startListener</redirect_url></proxy></message_header><message_body><query_id>2297</query_id><previous_message_id>12737</previous_message_id><client_secret>changeME!!!</client_secret></message_body></ns6:request>" -H"Content-Type: text/xml" http://localhost:9090/i2b2/services/SHRINEQEPService/getCRCMessage
	
	*/
	public OMElement getCRCMessage(OMElement requestElement) 
			throws I2B2Exception {
		poller.startIfNotRunning();
		LocalTime startTime = LocalTime.now();
		LocalTime returnTime = startTime.plusSeconds(5);
		
		int queryID = 0;
		int previousMessageID = 0;
		String clientSecret = "";
		
		Iterator<OMElement> it = requestElement.getChildElements();
		while (it.hasNext())
		{
			OMElement next = it.next();
			if("message_body".equals(next.getLocalName()))
			{
				Iterator<OMElement> it1 = next.getChildElements();
				while (it1.hasNext())
				{
					OMElement next1 = it1.next();
					if("query_id".equals(next1.getLocalName())) queryID = Integer.parseInt(next1.getText());
					if("previous_message_id".equals(next1.getLocalName())) previousMessageID = Integer.parseInt(next1.getText());
					if("client_secret".equals(next1.getLocalName())) clientSecret = next1.getText();
				}
			}
			
		}
		String x = null;
		
		while (LocalTime.now().isBefore(returnTime))
		{
			if(SHRINEHubPollService.latestMessage > previousMessageID)
			{
				x = SHRINEHubPollService.getCRCMessage(queryID, previousMessageID, clientSecret);
				if (x != null) break;
			}
			else
			{
				try{Thread.sleep(50);}
				catch(Exception e) {}
			}
		}
		
		OMElement returnElement;
		try{
			if(x == null)
			{
				returnElement = AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns6:request xmlns:ns4=\"http://www.i2b2.org/xsd/cell/crc/psm/1.1/\" xmlns:ns7=\"http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/\" xmlns:ns3=\"http://www.i2b2.org/xsd/cell/crc/pdo/1.1/\" xmlns:ns5=\"http://www.i2b2.org/xsd/hive/plugin/\" xmlns:ns2=\"http://www.i2b2.org/xsd/hive/pdo/1.1/\" xmlns:ns6=\"http://www.i2b2.org/xsd/hive/msg/1.1/\"><message_header><proxy><redirect_url>http://localhost:9090/i2b2/services/SHRINEQEPService/startListener</redirect_url></proxy></message_header><message_body><message_status>NO MESSAGE</message_status></message_body></ns6:request>");
			}
			else 
			{
				returnElement = AXIOMUtil.stringToOM("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns6:request xmlns:ns4=\"http://www.i2b2.org/xsd/cell/crc/psm/1.1/\" xmlns:ns7=\"http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/\" xmlns:ns3=\"http://www.i2b2.org/xsd/cell/crc/pdo/1.1/\" xmlns:ns5=\"http://www.i2b2.org/xsd/hive/plugin/\" xmlns:ns2=\"http://www.i2b2.org/xsd/hive/pdo/1.1/\" xmlns:ns6=\"http://www.i2b2.org/xsd/hive/msg/1.1/\"><message_header><proxy><redirect_url>http://localhost:9090/i2b2/services/SHRINEQEPService/startListener</redirect_url></proxy></message_header>" + x + "</ns6:request>");
			}
		}
		catch(Exception e) {log.info("getCRCMessage: " + e); returnElement = requestElement; }
		return returnElement;
	}

	private void traverseOMEElement(OMElement e, int i)
	{
		Iterator<OMElement> it = e.getChildElements();
		while (it.hasNext())
		{
			OMElement next = it.next();
			log.info("Level: " + i + "   " +  next.getLocalName() + "   " + next.getText());
			traverseOMEElement(next, i + 1);
		}
	}
		
}
