/******************************************************************************
 * Vige, Home of Professional Open Source Copyright 2010, Vige, and           *
 * individual contributors by the @authors tag. See the copyright.txt in the  *
 * distribution for a full listing of individual contributors.                *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may    *
 * not use this file except in compliance with the License. You may obtain    *
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0        *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/
package it.vige.albopretorio.ocr;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;

import org.alfresco.util.exec.RuntimeExec;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import it.vige.albopretorio.util.exec.RuntimeExecCommandProcessor;

// Transformer from plain PDF (images attached) to Searchable PDF by using OCR service
public class RestClient extends RuntimeExec  {
	
	public static final Integer EXIT_CODE_SUCCESS = 0;
	public static final Integer EXIT_CODE_ERROR = -1;
	
	private static final Log logger = LogFactory.getLog(RestClient.class);
	
	private String url;
	
	public ExecutionResult execute(Map<String, String> properties, long timeoutMs) {
		
		ExecutionResult executionResult = null;
		HttpClient httpClient = new HttpClient();
		
		try{
			
			httpClient.setHttpConnectionManager(new MultiThreadedHttpConnectionManager());        
			logger.info("Params: " + Arrays.asList(super.getCommand(properties)).toString().replaceAll(",", " "));
	
			String urlFull = url + 
					"language=" + super.getCommand(properties)[0] +
					"&source=" + URLEncoder.encode(super.getCommand(properties)[1], "UTF-8") +
			    	"&target=" + URLEncoder.encode(super.getCommand(properties)[2], "UTF-8");
						
			GetMethod method = new GetMethod(urlFull);
				
			int statusCode = httpClient.executeMethod(method);
					
			String stdOut = null;
			String stdErr = null;
			int exitCode = 0;
			
			if (statusCode == HttpStatus.SC_OK) {	
				stdOut = statusCode + " " + method.getStatusText();
				exitCode = EXIT_CODE_SUCCESS;
			} else {
				stdErr = statusCode + " " + method.getStatusText();
				exitCode = EXIT_CODE_ERROR;
			}
				
			method.releaseConnection();
			executionResult = RuntimeExecCommandProcessor.getInstance(null, super.getCommand(properties), null, exitCode, stdOut, stdErr);
			
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	   
		
		return executionResult;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}

}
