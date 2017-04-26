package com.rp.certificate.dto;

public class CertificatedDTO{
	private String token;
	private String secretKey;
	
	public CertificatedDTO(){}
	
	public CertificatedDTO(String token, String secretKey) {
		super();
		this.token = token;
		this.secretKey = secretKey;
	}
	
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
}
