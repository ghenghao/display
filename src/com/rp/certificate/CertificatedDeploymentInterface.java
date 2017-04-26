package com.rp.certificate;

import java.util.Date;
import java.util.Map;

import com.rp.certificate.domain.CertificatedDeploymentUser;

/** 
 * @Company
 * @author  Cooley
 * @function   the interface is for user statistics data of training status.
 * @version    1.0 
 * @Create  2016.11.08 4:11:42 
 */
public interface CertificatedDeploymentInterface {
	/**
	 * main exposed method called by PersistCertificatedDeploymentUserJob
	 * @return
	 */
	public Map<String, CertificatedDeploymentUser> getCertificatedDeploymentUserMap();
	
	/**
	 * main exposed method called by RPUI
	 * @return
	 */
	public Map<String, CertificatedDeploymentUser> getCertificatedDeploymentUserMap(
			Date startTime, Date endTime) ;
}
