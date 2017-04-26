package com.rp.certificate.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.log4j.Logger;

public class HttpClientUtil {
	private static final Logger logger = Logger.getLogger(HttpClientUtil.class);
	
	public static String httpClientRequest(String uri, String httpMethod, Map<String, String> headerMap, Map<String, String> paramsMap, String requestEntityStr) {
		String responseBody = null;

		int timeout = 5000;
		int retryCount = 3;
		
		while(retryCount > 0){
			HttpClient client = new HttpClient();
			HttpMethod method = httpMethod.equalsIgnoreCase("get") ? httpGetMethod(uri, headerMap, paramsMap)
					: httpPostMethod(uri, headerMap, paramsMap, requestEntityStr);
			try{
				// set timeout
				client.getHttpConnectionManager().getParams()
						.setConnectionTimeout(timeout);
			
				client.getHttpConnectionManager().getParams().setSoTimeout(2*timeout); // get response timeout
		
				// retry policy
//				method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
//						new DefaultHttpMethodRetryHandler());
		
				int statusCode = client.executeMethod(method);
				if (statusCode != HttpStatus.SC_OK) {
					logger.error("Method failed: " + method.getStatusLine());
				}
	
				// deal with redirect
				// 301 or 302
				if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
						|| statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
					// get the redirect url from the header
					Header locationHeader = method.getResponseHeader("location");
					String location = null;
					if (locationHeader != null) {
						location = locationHeader.getValue();
						logger.info("The page was redirected to:" + location);
					} else {
						logger.error("Location field value is null.");
					}
				}
	
				// get response
				responseBody = method.getResponseBodyAsString();
				break;
			} catch (HttpException e) {
				logger.error("Please check provided http address! " + e);
				throw new RuntimeException(e);
			} catch (IOException e) {
				retryCount--;
				timeout = timeout * 2;
				logger.error("connection error, retry " + retryCount + ",:" + e);
				if(retryCount <= 0) {
					throw new RuntimeException(e);
				}
			} finally {
				method.releaseConnection();
			} 
		}
		
		return responseBody;
	}
	
	
	private static HttpMethod httpGetMethod(String uri, Map<String, String> headerMap, Map<String, String> paramsMap) {
		StringBuilder sbu = new StringBuilder(uri);
		//params
		if(paramsMap != null && paramsMap.entrySet().size() > 0){
			sbu.append("?");
			for(Map.Entry<String, String> param : paramsMap.entrySet()){
				sbu.append(param.getKey() + "=" + param.getValue() + "&");
			}
			uri = sbu.substring(0, sbu.length()-1);
		}
		
		HttpMethod method = new GetMethod(uri);
		
		// header
		method.setRequestHeader("connection", "Keep-Alive");
		method.setRequestHeader("user-agent",
				"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
		if(headerMap != null){
			for(Map.Entry<String, String> header : headerMap.entrySet()){
				method.setRequestHeader(header.getKey(), header.getValue());
			}
		}
		
		return method;
	}
	
	
	private static HttpMethod httpPostMethod(String uri, Map<String, String> headerMap, Map<String, String> paramsMap, String requestEntityStr) {
		PostMethod method = new PostMethod(uri);
		// header
		method.setRequestHeader("connection", "Keep-Alive");
		method.setRequestHeader("user-agent",
				"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
		if(headerMap != null){
			for(Map.Entry<String, String> header : headerMap.entrySet()){
				method.setRequestHeader(header.getKey(), header.getValue());
			}
		}
		
		// post params
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		
		if(paramsMap != null && paramsMap.entrySet().size() > 0){
			for(Map.Entry<String, String> param : paramsMap.entrySet()){
				nameValuePairs.add(new NameValuePair(param.getKey(), param.getValue()));
			}
			method.setRequestBody(nameValuePairs.toArray(new NameValuePair[nameValuePairs.size()]));
		}
		
		//requestEntity
		if(requestEntityStr != null && !requestEntityStr.trim().equals("")){
			try {
				byte[] bytes = requestEntityStr.getBytes("utf-8");
				InputStream inputStream = new ByteArrayInputStream(bytes, 0, bytes.length);
				RequestEntity requestEntity = new InputStreamRequestEntity(inputStream, bytes.length, "application/soap+xml; charset=utf-8");
				method.setRequestEntity(requestEntity);
			} catch (UnsupportedEncodingException e) {
				logger.error("transfer to utf8 error", e);
				throw new RuntimeException("transfer to utf8 error", e);
			}
		}
		
		return method;
	}
}
