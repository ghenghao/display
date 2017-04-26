package com.rp.certificate;


import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rp.certificate.domain.CertificatedDeploymentUser;
import com.rp.certificate.dto.CertificatedDTO;
import com.rp.exception.BusinessUtilsRuntimeException;

public abstract class  AbstractCertificatedDeployment implements CertificatedDeploymentInterface{
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractCertificatedDeployment.class);
	/**
	 * main exposed method called by PersistCertificatedDeploymentUserJob
	 * @return
	 */
	public Map<String, CertificatedDeploymentUser> getCertificatedDeploymentUserMap(){
		return getCertificatedDeploymentUserMap(null, null);
	}
	
	/**
	 * main exposed method called by RPUI
	 * @return
	 */
	public Map<String, CertificatedDeploymentUser> getCertificatedDeploymentUserMap(
			Date startTime, Date endTime) {
		try{
			//call auth token api, to get session token and secretKey
			CertificatedDTO certificatedDTO = genAuthToken();
			
			if(certificatedDTO == null) {
				logger.error("call auth token api and no response");
				throw new BusinessUtilsRuntimeException("call auth token api and no response");
			}
			
			//call transcript api and get those users who pass the test
			Map<String, CertificatedDeploymentUser> certificatedDeploymentUserMap = transSearch(certificatedDTO.getToken(), certificatedDTO.getSecretKey(), startTime, endTime);
			
			if(certificatedDeploymentUserMap == null) {
				logger.error("call atranscript api and no response");
				throw new BusinessUtilsRuntimeException("call atranscript api and no response");
			}
			
			//call getUser SOAP api and get user NtId and name information
			Map<String, CertificatedDeploymentUser> certificatedDeploymentResultMap = getUserInfos(certificatedDeploymentUserMap);
			return certificatedDeploymentResultMap;
		} catch (Exception e){
			throw new BusinessUtilsRuntimeException("CertificatedDeployment run exception", e);
		}
	}
	
	/**
	 * Generate the CSOD token and secret key
	 * @return
	 */
	public abstract CertificatedDTO genAuthToken();
	
	/**
	 * Transcript Search with date input, for RPUI admin use
	 * @param token
	 * @param secretKey
	 * @param modificationStartDateStr
	 * @param modificationEndDateStr
	 * @return
	 */
	public abstract Map<String, CertificatedDeploymentUser> transSearch(String token, String secretKey, Date modificationStartDate, Date modificationEndDate) ;
	
	/**
	 * transfer transcrip user to Nt user info
	 * @param certificatedDeploymentUserMap
	 * @return
	 */
	public abstract Map<String, CertificatedDeploymentUser> getUserInfos(Map<String, CertificatedDeploymentUser> certificatedDeploymentUserMap);
	
}




