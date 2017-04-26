package com.rp.certificate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rp.certificate.domain.CertificatedDeploymentUser;
import com.rp.certificate.dto.CertificatedDTO;
import com.rp.certificate.utils.DateUtil;
import com.rp.certificate.utils.HttpClientUtil;

@Component
public class CertificatedDeploymentUserUtil extends AbstractCertificatedDeployment{
	
	private static final Logger logger = LoggerFactory.getLogger(CertificatedDeploymentUserUtil.class);
	
	private static final String HEADER_API_KEY = "x-csod-api-key";
	private static final String HEADER_SESSION_TOKEN = "x-csod-session-token";
	private static final String HEADER_CSOD_DATE = "x-csod-date";
	private static final String HEADER_CSOD_SIGNATURE = "x-csod-signature";
	private static final String PARAMS_LOID_NAME = "LOID";
	private static final int PROCESSOR_NUMBER = Runtime.getRuntime().availableProcessors();
	
	@Value("#{applicationProperties['certificateddeploymentuser.getuser.WSDL.url']}")
	private  String GETUSER_WSDL_URL;
	
	@Value("#{applicationProperties['certificateddeploymentuser.transcript.loid']}")
	private  String PARAMS_LOID_VALUE;
	
	@Value("#{applicationProperties['certificateddeploymentuser.token.url']}")
	private  String GEN_TOKEN_URL;
	
	@Value("#{applicationProperties['certificateddeploymentuser.token.url.suffix']}")
	private  String GEN_TOKEN_URL_SUFFIX;
	
	@Value("#{applicationProperties['certificateddeploymentuser.token.api.id']}")
	private  String API_ID;
	
	@Value("#{applicationProperties['certificateddeploymentuser.token.api.secret']}")
	private  String API_SECRET;
	
	@Value("#{applicationProperties['certificateddeploymentuser.transcript.url']}")
	private  String TRANS_SEARCH_URL;
	
	@Value("#{applicationProperties['certificateddeploymentuser.transcript.url.suffix']}")
	private  String TRANS_SEARCH_URL_SUFFIX;
	
	@Value("#{applicationProperties['certificateddeploymentuser.corp.name']}")
	private  String CORP_NAME;
	
	@Value("#{applicationProperties['certificateddeploymentuser.user.name']}")
	private  String USER_NAME;
	
	@Value("#{applicationProperties['certificateddeploymentuser.user.password']}")
	private  String PASSWORD;
	
	@Value("#{applicationProperties['certificateddeploymentuser.qualified.score']}")
	private  int QUALIFIED_SCORE;	//the score to pass the test
	
	@Value("#{applicationProperties['certificateddeploymentuser.user.valid.days']}")
	private  int CERTIFICATED_USER_DAY_SPAN;	//the test result lasts days 
	
	@Value("#{applicationProperties['certificateddeploymentuser.transcript.hours']}")
	private  int TRANS_SEARCH_HOUR_SPAN;	//transSearch time-span
	
	/**
	 * Generate the CSOD token and secret key
	 * @return
	 */
	public CertificatedDTO genAuthToken() {
		Map<String, String> headerMap = new HashMap<String, String>();

		//build request header
		Map<String, String> authTokenHeaderMap = getAuthTokenHeaderParams();
		headerMap.put("Accept", "text/xml");
		headerMap.put("Content-Type", "text/xml");
		headerMap.put(HEADER_API_KEY, authTokenHeaderMap.get(HEADER_API_KEY));
		headerMap.put(HEADER_CSOD_DATE, authTokenHeaderMap.get(HEADER_CSOD_DATE));
		headerMap.put(HEADER_CSOD_SIGNATURE, authTokenHeaderMap.get(HEADER_CSOD_SIGNATURE));
		
		//build request uri
		String uri = GEN_TOKEN_URL + "?userName={userName}&alias={alias}".replace("{userName}", USER_NAME).replace("{alias}", genAlias());
		
		String responseStr = HttpClientUtil.httpClientRequest(uri, "POST", headerMap, null, null);
		
		//extract token and secret key from responseXml
		XmlHelper xmlHelper = new XmlHelper(responseStr);
		String status = xmlHelper.getNodeText("status");
		if(Integer.parseInt(status) == 201) {
			String token = xmlHelper.getNodeText("\\data\\Session\\Token");
			String secretKey = xmlHelper.getNodeText("\\data\\Session\\Secret");
//			String expiresTime = xmlHelper.getNodeText("\\data\\Session\\ExpiresOn");
			
			if(StringUtils.isNotEmpty(token) && StringUtils.isNotEmpty(secretKey)) {
				return new CertificatedDTO(token, secretKey);
			} else {
				logger.error("didn't get the token or secretKey : " + token + "," + secretKey);
				throw new RuntimeException("didn't get the token or secretKey : " + token + "," + secretKey);
			}
		}else {
			logger.error("request failed, status is not 201 : " + responseStr);
			throw new RuntimeException("request failed, status is not 201 : " + responseStr);
		}
	}
	
	/**
	 * Generate the request header to get CSOD token and secret key,
	 * most important is SHA512 signature.
	 * @return
	 */
	private Map<String, String> getAuthTokenHeaderParams() {
		Map<String, String> resultMap = new HashMap<String, String>();
		
		String signatureCode = "";
		
		String httpMethod = "POST";
		String signTime = DateUtil.convertDate2SignString(new Date());
		
		StringBuilder sbuToSign = new StringBuilder();
		sbuToSign.append(httpMethod).append("\n")
				.append(HEADER_API_KEY + ":" + API_ID).append("\n")
				.append(HEADER_CSOD_DATE + ":" + signTime).append("\n")
				.append(GEN_TOKEN_URL_SUFFIX);
		
		try {
			signatureCode = HmacEncrypt.encryptHMACtoBase64_SHA512(sbuToSign.toString(), API_SECRET);
		} catch (Exception e) {
			logger.error("generate AuthToken's signToken failed", e);
			return null;
		}
		
		resultMap.put(HEADER_CSOD_DATE, signTime);
		resultMap.put(HEADER_API_KEY, API_ID);
		resultMap.put(HEADER_CSOD_SIGNATURE, signatureCode);
		
		return resultMap;
	}
	
	/**
	 * Transcript Search without date input, for the crons job
	 * @param token
	 * @param secretKey
	 * @return
	 */
	public  Map<String, CertificatedDeploymentUser> transSearch(String token, String secretKey){
		Map<String, CertificatedDeploymentUser> certificatedDeploymentUserMap = new HashMap<String, CertificatedDeploymentUser>();
		
		//modify time to match the request format
		String modificationStartDateStr = DateUtil.getGMTTimeString(DateUtil.addHour(new Date(), 0-TRANS_SEARCH_HOUR_SPAN)).replace(" ", "T");
		String modificationEndDateStr = DateUtil.getGMTTimeString(new Date()).replace(" ", "T");
		
		certificatedDeploymentUserMap = transSearch(token, secretKey, modificationStartDateStr, modificationEndDateStr);
		return certificatedDeploymentUserMap;
	}
	
	/**
	 * Transcript Search with date input, for RPUI admin use
	 * @param token
	 * @param secretKey
	 * @param modificationStartDateStr
	 * @param modificationEndDateStr
	 * @return
	 */
	public Map<String, CertificatedDeploymentUser> transSearch(String token, String secretKey, Date modificationStartDate, Date modificationEndDate) {
		if(modificationStartDate == null && modificationEndDate == null) {
			return  transSearch(token, secretKey);
		}
		
		Map<String, CertificatedDeploymentUser> certificatedDeploymentUserMap = new HashMap<String, CertificatedDeploymentUser>();

		Date currentTime = new Date();
		Date transModificationStartDate = modificationStartDate;
		
		//split the time span for 24 hours and transSearch
		while(transModificationStartDate.before(modificationEndDate)) {
			Date transModificationEndDate = DateUtil.addHour(transModificationStartDate, 24);
			if(transModificationEndDate.after(currentTime)) {
				transModificationEndDate = currentTime;
			}
			
			Date transModificationEndDate_GMT = DateUtil.getGMTTime(transModificationEndDate);
			
			if(transModificationEndDate_GMT != null && transModificationEndDate_GMT.after(modificationEndDate)){
				transModificationEndDate = modificationEndDate;
			}
			
			
			String transModificationStartDateStr = DateUtil.getGMTTimeString(transModificationStartDate).replace(" ", "T");
			String transModificationEndDateStr = DateUtil.getGMTTimeString(transModificationEndDate).replace(" ", "T");
			
			certificatedDeploymentUserMap.putAll(transSearch(token, secretKey, transModificationStartDateStr, transModificationEndDateStr));
			
			transModificationStartDate = DateUtil.addHour(transModificationStartDate, 24);
		}
		
		//if startDate == endDate
		if(!transModificationStartDate.before(modificationEndDate)) {
			String transModificationStartDateStr = DateUtil.getGMTTimeString(transModificationStartDate).replace(" ", "T");
			Date transModificationEndDate = DateUtil.addHour(transModificationStartDate, 24);
			String transModificationEndDateStr = DateUtil.getGMTTimeString(transModificationEndDate).replace(" ", "T");
			
			certificatedDeploymentUserMap.putAll(transSearch(token, secretKey, transModificationStartDateStr, transModificationEndDateStr));
		}
		
		return certificatedDeploymentUserMap;
	}
	
	/**
	 * the transSearch method with multi-thread, the main transSearch method.
	 * @param certificatedDeploymentUserMap
	 * @param token
	 * @param secretKey
	 * @param modificationStartDateStr
	 * @param modificationEndDateStr
	 */
	private Map<String, CertificatedDeploymentUser> transSearch(final String token, final String secretKey, 
			final String modificationStartDateStr, final String modificationEndDateStr) {
		Map<String, CertificatedDeploymentUser> certificatedDeploymentUserMap = new HashMap<String, CertificatedDeploymentUser>();
		
		int timeout = 5 * 60 * 1000;
		String timeSpanMsg = " time is from " + modificationStartDateStr + " to " + modificationEndDateStr + "\n";
		
		int pageTotal = transSearchPageTotal(token, secretKey, modificationStartDateStr, modificationEndDateStr);
		if(pageTotal == -1) {
			logger.error("can not get total page number");
        	throw new RuntimeException("can not get total page number");
		} else if(pageTotal == 0) {
			logger.info("no data in transcript Search, " + timeSpanMsg);
			return certificatedDeploymentUserMap;
		}
		
		//use a thread pool to manage the multi-threads request
		ExecutorService pool;
		if(PROCESSOR_NUMBER > 0 && pageTotal > PROCESSOR_NUMBER*2){
			pool = Executors.newFixedThreadPool(PROCESSOR_NUMBER*2);
		} else {
			pool = Executors.newFixedThreadPool(pageTotal);
		}
		
		CompletionService<Map<Integer, Map<String, CertificatedDeploymentUser>>> completionService 
					= new ExecutorCompletionService<Map<Integer, Map<String, CertificatedDeploymentUser>>>(pool);
		
		int startPageNumber = 1;
		int endPageNumber = pageTotal;
		
		//every request get the user data on a page
		for(int i=startPageNumber; i<=endPageNumber; i++){
			final int pageNumber = i;
			
			completionService.submit(new Callable<Map<Integer, Map<String, CertificatedDeploymentUser>>>() {
				@Override
				public Map<Integer, Map<String, CertificatedDeploymentUser>> call() throws Exception {
					Map<Integer, Map<String, CertificatedDeploymentUser>> certificatedDeploymentUserMap = new HashMap<Integer, Map<String,CertificatedDeploymentUser>>();
					try {
						certificatedDeploymentUserMap.put(pageNumber, transSearch(token, secretKey, pageNumber, modificationStartDateStr, modificationEndDateStr));
					} catch (Exception e) {
						String errMsg = "error, pageNumber:" + pageNumber + e.getMessage();
						Map<String, CertificatedDeploymentUser> errMsgMap = new HashMap<String, CertificatedDeploymentUser>();
						errMsgMap.put(errMsg, null);
						certificatedDeploymentUserMap.put(pageNumber, errMsgMap);
					}
					
					return certificatedDeploymentUserMap;
				}
			});
		}
		
		String failMsg = "exec fail, " + timeSpanMsg;
		Map<Integer, Map<String, CertificatedDeploymentUser>> allThreadRetMap = new HashMap<Integer, Map<String,CertificatedDeploymentUser>>();
		
		//get execute result of all threads
		try{
			for(int i=startPageNumber; i<=endPageNumber; i++){
				Map<Integer, Map<String, CertificatedDeploymentUser>> singleThreadRetMap = null;
	            try {
	                 Future<Map<Integer, Map<String, CertificatedDeploymentUser>>> f 
	                 			= completionService.poll(timeout, TimeUnit.MILLISECONDS);
	                 if(f != null){
	                	 singleThreadRetMap = f.get(timeout, TimeUnit.MILLISECONDS);
	                	 allThreadRetMap.putAll(singleThreadRetMap);
	                 }
	                 
	            } catch (InterruptedException e) {
	            	logger.error(failMsg, e);
	            	pool.shutdownNow();
	            	throw new RuntimeException(failMsg, e);
	            } catch (ExecutionException e) {
	            	logger.error(failMsg, e);
	            	pool.shutdownNow();
	            	throw new RuntimeException(failMsg, e);
	            } catch (TimeoutException e) {
	            	logger.error(failMsg, e);
	            	pool.shutdownNow();
	            	throw new RuntimeException(failMsg, e);
	            }
			}
		} finally{
        	pool.shutdown();
        	try {
        		// wait for pool shutdown
				while(!pool.awaitTermination(5, TimeUnit.SECONDS)) {
					Thread.sleep(100);
				};
			} catch (InterruptedException e) {
				logger.error("shutdown thread pool failed", e);
			}
		}
		
		for(int i=startPageNumber; i<=endPageNumber; i++) {
			Map<String, CertificatedDeploymentUser> tempMap = allThreadRetMap.get(i);
			if(tempMap != null && tempMap.size() > 1){
		       	certificatedDeploymentUserMap.putAll(tempMap);
		    } else if(tempMap != null && tempMap.size() == 1){
		       	String[] keys = tempMap.keySet().toArray(new String[]{});
		       	if(keys[0].startsWith("error")) {
			       	logger.error(failMsg + keys[0]);
			       	throw new RuntimeException(failMsg + keys[0]);
			    } else {
			    	certificatedDeploymentUserMap.putAll(tempMap);
			    }
		   }
		}
		
		allThreadRetMap.clear();	//help GC
		
		return certificatedDeploymentUserMap;
	}
	
	/**
	 * try to call Transcript Search API, and return the totalPage number.
	 * @param token
	 * @param secretKey
	 * @param modificationStartDateStr
	 * @param modificationEndDateStr
	 * @return
	 */
	private int transSearchPageTotal(String token, String secretKey, String modificationStartDateStr, String modificationEndDateStr){
		
		Map<String, String> headerMap = new HashMap<String, String>();
		Map<String, String> paramsMap = new HashMap<String, String>();
		
		//generate the request header
		Map<String, String> transSearchHeaderMap = getTransSearchHeaderParams(token, secretKey);
		headerMap.put("Accept", "text/xml");
		headerMap.put("Content-Type", "text/xml");
		headerMap.put(HEADER_CSOD_DATE, transSearchHeaderMap.get(HEADER_CSOD_DATE));
		headerMap.put(HEADER_SESSION_TOKEN, transSearchHeaderMap.get(HEADER_SESSION_TOKEN));
		headerMap.put(HEADER_CSOD_SIGNATURE, transSearchHeaderMap.get(HEADER_CSOD_SIGNATURE));
		
		paramsMap.put(PARAMS_LOID_NAME, PARAMS_LOID_VALUE);
		paramsMap.put("ModificationStartDate", modificationStartDateStr);
		paramsMap.put("ModificationEndDate", modificationEndDateStr);
		
		//send request
		String responseStr = HttpClientUtil.httpClientRequest(TRANS_SEARCH_URL, "GET", headerMap, paramsMap, null);
		
		//extract result from the response xml
		XmlHelper xmlHelper = new XmlHelper(responseStr);
		String status = xmlHelper.getNodeText("status");
		String result = xmlHelper.getNodeText("\\data\\LOTranscriptItemResponse\\Result");
		
		if(Integer.parseInt(status) == 200) {
			//if response is more than 1 page
			Element transcriptItemNode = xmlHelper.getSingleNode("\\data\\LOTranscriptItemResponse\\TranscriptItem");
			String totalPageStr = xmlHelper.getNodeTextFromNode("\\TotalPages", transcriptItemNode);
			if(StringUtils.isNumeric(totalPageStr)){
				return Integer.parseInt(totalPageStr);
			}
		} else {
			logger.error("transSearch status is not 200, " + responseStr);
			throw new RuntimeException("transSearch status is not 200, " + responseStr);
		}
		
		return -1;
	}
	
	/**
	 * Call Transcript Search API of one page
	 * @param certificatedDeploymentUserMap [key: transUserId, value: detail info]
	 * @param token
	 * @param secretKey
	 * @param pageNumber
	 * @param modificationStartDateStr
	 * @param modificationEndDateStr
	 */
	private Map<String, CertificatedDeploymentUser> transSearch(String token, String secretKey, int pageNumber, String modificationStartDateStr, String modificationEndDateStr) {
		Map<String, CertificatedDeploymentUser> certificatedDeploymentUserMap = new HashMap<String, CertificatedDeploymentUser>();
		
		Map<String, String> headerMap = new HashMap<String, String>();
		Map<String, String> paramsMap = new HashMap<String, String>();
		
		//generate the request header
		Map<String, String> transSearchHeaderMap = getTransSearchHeaderParams(token, secretKey);
		headerMap.put("Accept", "text/xml");
		headerMap.put("Content-Type", "text/xml");
		headerMap.put(HEADER_CSOD_DATE, transSearchHeaderMap.get(HEADER_CSOD_DATE));
		headerMap.put(HEADER_SESSION_TOKEN, transSearchHeaderMap.get(HEADER_SESSION_TOKEN));
		headerMap.put(HEADER_CSOD_SIGNATURE, transSearchHeaderMap.get(HEADER_CSOD_SIGNATURE));
		
		paramsMap.put(PARAMS_LOID_NAME, PARAMS_LOID_VALUE);
		paramsMap.put("PageNumber", String.valueOf(pageNumber));
		paramsMap.put("ModificationStartDate", modificationStartDateStr);
		paramsMap.put("ModificationEndDate", modificationEndDateStr);
		
		//send request
		String responseStr = HttpClientUtil.httpClientRequest(TRANS_SEARCH_URL, "GET", headerMap, paramsMap, null);
		
		//extract result from the response xml
		XmlHelper xmlHelper = new XmlHelper(responseStr);
		String status = xmlHelper.getNodeText("status");
		String result = xmlHelper.getNodeText("\\data\\LOTranscriptItemResponse\\Result");
		
		if(Integer.parseInt(status) == 200) {
			List<Element> infoNodes = xmlHelper.getNodeList("\\data\\LOTranscriptItemResponse\\TranscriptItem\\Transcripts\\Transcript");
			
			for(Element infoNode : infoNodes) {
				CertificatedDeploymentUser certificatedDeploymentUser = new CertificatedDeploymentUser();
				String transcriptUserId = xmlHelper.getNodeTextFromNode("\\UserID", infoNode);
				String transcriptStatus = xmlHelper.getNodeTextFromNode("\\TranscriptStatus", infoNode);
				String completionDateStr = xmlHelper.getNodeTextFromNode("\\ActionDates\\CompletionDate", infoNode);
				String modificationDateStr = xmlHelper.getNodeTextFromNode("\\ActionDates\\ModificationDate", infoNode);
				String score = xmlHelper.getNodeTextFromNode("\\Score", infoNode);
				
				// a User may occur many times in the response xml, make sure the right status not be override.
				if(StringUtils.isNotBlank(transcriptUserId) && StringUtils.isNotBlank(transcriptStatus)){
					boolean b = StringUtils.isNotBlank(completionDateStr) && StringUtils.isNotBlank(modificationDateStr)
							&& StringUtils.isNumeric(score) 
//							&& transcriptStatus.equalsIgnoreCase("Completed")
							&& Integer.parseInt(score) >= QUALIFIED_SCORE;
					if(b) {
						//check if the user passed the test
						Date completionDate = DateUtil.convertTransSearchTimeString2Date(completionDateStr);
						completionDate = DateUtil.addHour(completionDate, 1);	//don't know why have to add one hour, just follow the Release Rigor report
						Date validTillDate = DateUtil.addDay(completionDate, CERTIFICATED_USER_DAY_SPAN);
						certificatedDeploymentUser.setCompletionDate(completionDate);	//GMT DATE or SERVER DATE?
						certificatedDeploymentUser.setValidTillDate(validTillDate);
						certificatedDeploymentUserMap.put(transcriptUserId, certificatedDeploymentUser);
					} else if(transcriptStatus.equalsIgnoreCase("Started") ){
						certificatedDeploymentUserMap.put(transcriptUserId, certificatedDeploymentUser);
					}
				} else {
					logger.error("transcriptUserId or transcriptStatus is blank, check the Transcript Search API response String. transcriptUserId : " + transcriptUserId);
				}
			}
			
		} else {
			logger.error("transSearch status is not 200, " + responseStr);
			throw new RuntimeException("transSearch status is not 200, " + responseStr);
		}
		
		return certificatedDeploymentUserMap;
	}
	
	
	/**
	 * generate request header for TransSearch
	 * most important is SHA512 signature.
	 * @param sessionToken
	 * @param sessionTokenSecret
	 * @return
	 */
	private Map<String, String> getTransSearchHeaderParams(String sessionToken, String sessionTokenSecret) {
		Map<String, String> resultMap = new HashMap<String, String>();
		
		String signatureCode = "";
		
		String httpMethod = "GET";
		String signTime = DateUtil.covertDate2TranscriptString(new Date());
		
		StringBuilder sbuToSign = new StringBuilder();
		sbuToSign.append(httpMethod).append("\n")
				.append(HEADER_CSOD_DATE + ":" + signTime).append("\n")
				.append(HEADER_SESSION_TOKEN + ":" + sessionToken).append("\n")
				.append(TRANS_SEARCH_URL_SUFFIX);
		
		try {
			signatureCode = HmacEncrypt.encryptHMACtoBase64_SHA512(sbuToSign.toString(), sessionTokenSecret);
		} catch (UnsupportedEncodingException e) {
			logger.error("generate TransSearch's signToken failed", e);
			return null;
		}
		
		resultMap.put(HEADER_CSOD_DATE, signTime);
		resultMap.put(HEADER_SESSION_TOKEN, sessionToken);
		resultMap.put(HEADER_CSOD_SIGNATURE, signatureCode);
		
		return resultMap;
	}
	
	/**
	 * transfer transcrip user to Nt user info
	 * @param certificatedDeploymentUserMap
	 * @return
	 */
	public Map<String, CertificatedDeploymentUser> getUserInfos(Map<String, CertificatedDeploymentUser> certificatedDeploymentUserMap){
		if(certificatedDeploymentUserMap == null || certificatedDeploymentUserMap.size() == 0) { 
			logger.info("no data in certificatedDeploymentUserMap");
			return null;
		}
		
		int thresholdValue = 500;
		
		Map<String, CertificatedDeploymentUser> userMap = new HashMap<String, CertificatedDeploymentUser>();
		
		Map<String, String> headerMap = new HashMap<String, String>();
		
		headerMap.put("Accept", "text/xml");
		headerMap.put("Content-Type", "text/xml");
		headerMap.put("SOAPAction", "GetUser");
		
		//deal with too many user data
		if(certificatedDeploymentUserMap.size() < thresholdValue) {
			getCertificatedDeploymentUserMap(certificatedDeploymentUserMap, userMap, certificatedDeploymentUserMap.keySet(), headerMap);
		} else {
			List<String> certificatedDeploymentUserMapKeyList = new ArrayList<String>(certificatedDeploymentUserMap.keySet());
			
			int fromIndex = 0;
			
			while(fromIndex < certificatedDeploymentUserMapKeyList.size()){
				int toIndex = fromIndex + thresholdValue;
				if(toIndex > certificatedDeploymentUserMapKeyList.size()){
					toIndex = certificatedDeploymentUserMapKeyList.size();
				}
				
				List<String> certificatedDeploymentUserMapKey_SubList = certificatedDeploymentUserMapKeyList.subList(fromIndex, toIndex);
				
				getCertificatedDeploymentUserMap(certificatedDeploymentUserMap, userMap, certificatedDeploymentUserMapKey_SubList, headerMap);
				
				fromIndex = fromIndex + thresholdValue;
			}
		}
		
		//clear old Map
		certificatedDeploymentUserMap.clear();	//help GC
		
		return userMap;
	}
	
	/**
	 * transfer transcrip user to Nt user info
	 */
	private void getCertificatedDeploymentUserMap(Map<String, CertificatedDeploymentUser> certificatedDeploymentUserMap,
			Map<String, CertificatedDeploymentUser> userMap,
			Collection<String> userIdCol, 
			Map<String, String> headerMap){
		String userIdStr = StringUtils.collectionToString(userIdCol);
		String requestEntityStr = StringUtils.genSoapRequestXml(userIdStr);
		
		String responseStr = HttpClientUtil.httpClientRequest(GETUSER_WSDL_URL, "POST", headerMap, null, requestEntityStr);

		XmlHelper xmlHelper = new XmlHelper(responseStr);
		List<Element> infoNodes = xmlHelper.getNodeList("\\Body\\GetUserResponse\\User");
		for(Element infoNode : infoNodes) {
			String userId = xmlHelper.getAttributeValue("Id", infoNode);
			
			String userFirstName = xmlHelper.getAttributeValueFromNode("\\Contact\\Name@First", infoNode);
			String userLastName = xmlHelper.getAttributeValueFromNode("\\Contact\\Name@Last", infoNode);
//			String userEmail = xmlHelper.getNodeTextFromNode("\\User\\Contact\\Email", infoNode);
			String userNtId = xmlHelper.getNodeTextFromNode("\\Authentication\\Username", infoNode);//or from the Email?
			String userName = userLastName + "," + userFirstName;
			
			CertificatedDeploymentUser certificatedDeploymentUser = certificatedDeploymentUserMap.get(userId);
			if(certificatedDeploymentUser != null){
				certificatedDeploymentUser.setUserNtId(userNtId);
				certificatedDeploymentUser.setUserName(userName);
				certificatedDeploymentUser.setFirstName(userFirstName);
				certificatedDeploymentUser.setLastName(userLastName);
			}
			
			//new Map
			userMap.put(userNtId, certificatedDeploymentUser);
		}
	}
	
	
	/**
	 * generate UUID string
	 */
	public static String genAlias(){
		String s = UUID.randomUUID().toString(); 
		//delete the '-' in the 's' 
        return s.substring(0,8)+s.substring(9,13)+s.substring(14,18)+s.substring(19,23)+s.substring(24); 
	}
}




