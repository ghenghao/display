package com.rp.certificate.domain;

import java.util.Date;

public class CertificatedDeploymentUser{
	
    private long id;
    
	private String userNtId;
    
    private String userName;
    
    private String firstName;
    
    private String lastName;
    
    private Date completionDate;
    
    private Date validTillDate;
    
    private String lastOperator;
    
    private String lastOperatorNote;
    
    private String lastUpdateTime;
    
    private boolean isDeleted;
    
    private Date insertTime;
    
    public String getUserNtId() {
		return userNtId;
	}

	public void setUserNtId(String userNtId) {
		this.userNtId = userNtId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Date getCompletionDate() {
		return completionDate;
	}

	public void setCompletionDate(Date completionDate) {
		this.completionDate = completionDate;
	}

	public Date getValidTillDate() {
		return validTillDate;
	}

	public void setValidTillDate(Date validTillDate) {
		this.validTillDate = validTillDate;
	}

	public String getLastOperator() {
		return lastOperator;
	}

	public void setLastOperator(String lastOperator) {
		this.lastOperator = lastOperator;
	}

	public String getLastOperatorNote() {
		return lastOperatorNote;
	}

	public void setLastOperatorNote(String lastOperatorNote) {
		this.lastOperatorNote = lastOperatorNote;
	}

	public String getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(String lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public Date getInsertTime() {
		return insertTime;
	}

	public void setInsertTime(Date insertTime) {
		this.insertTime = insertTime;
	}

	@Override
	public String toString() {
		return "CertificatedDeploymentUser [id=" + id + ", userNtId="
				+ userNtId + ", userName=" + userName + ", firstName="
				+ firstName + ", lastName=" + lastName + ", completionDate="
				+ completionDate + ", validTillDate=" + validTillDate
				+ ", lastOperator=" + lastOperator + ", lastOperatorNote="
				+ lastOperatorNote + ", lastUpdateTime=" + lastUpdateTime
				+ ", isDeleted=" + isDeleted + ", insertTime=" + insertTime
				+ "]";
	}
	
}
