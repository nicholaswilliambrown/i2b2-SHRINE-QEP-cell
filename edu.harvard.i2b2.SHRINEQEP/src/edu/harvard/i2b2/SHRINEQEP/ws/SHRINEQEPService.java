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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import edu.harvard.i2b2.SHRINEQEP.util.SHRINEHubPollService;
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
	public OMElement helloWorld(OMElement helloElement) 
			throws I2B2Exception {
		log.info("\n\n\n\n\n\n\n\n\n\n\n\n\n Hello World!!! \n\n\n\n\n\n\n\n\n\n\n\n\n");
		poller.startIfNotRunning();
		return helloElement;

	}


}
