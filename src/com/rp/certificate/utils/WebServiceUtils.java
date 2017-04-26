package com.rp.certificate.utils;

import org.apache.commons.lang.StringUtils;

public class WebServiceUtils {
	/**
	 * for getUser webservice
	 */
	public String genSoapRequestXml(String userIds){
		if(StringUtils.isBlank(userIds)) { 
			return null;
		}
		
		String soapRequestData = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<soapenv:Envelope  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:Cornerstone:ClientDataService\">"
				+  "<soapenv:Header>"
				+ "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">"
				+ "<wsse:UsernameToken>"
				+ "<wsse:Username>[CORPNAME]\\[WEBSERVICEACCOUNT]</wsse:Username>"
				+ "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">[PASSWORD]</wsse:Password>"
				+ "</wsse:UsernameToken>"
				+ "</wsse:Security>"
				+ "</soapenv:Header>"
				+    "<soapenv:Body>"
				+       "<urn:GetUser>"
				+          "<!--Optional:-->"
				+         "<urn:strUserId>[USERIDS]</urn:strUserId>"
				+      "</urn:GetUser>"
				+    "</soapenv:Body>"
				+ "</soapenv:Envelope>";
		soapRequestData = soapRequestData.replace("[CORPNAME]", CORP_NAME)
							.replace("[WEBSERVICEACCOUNT]", USER_NAME)
							.replace("[PASSWORD]", PASSWORD)
							.replace("[USERIDS]", userIds);
		
		
		return soapRequestData;
	}
}
