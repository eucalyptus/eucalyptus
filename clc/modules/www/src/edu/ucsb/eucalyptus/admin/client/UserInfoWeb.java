/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by IntelliJ IDEA.
 * User: dmitriizagorodnov
 * Date: May 3, 2008
 * Time: 3:35:41 PM
 * To change this template use File | Settings | File Templates.
 */

public class UserInfoWeb implements IsSerializable
{
    private String userName;
    private String realName;
    private String emailAddress;
    private String bCryptedPassword;
    private String telephoneNumber;
    private String affiliation;
    private String projectDescription;
    private String projectPIName;
    private String confirmationCode;
    private String certificateCode;
    private Boolean isApproved;
    private Boolean isConfirmed;
    private Boolean isEnabled;
    private Boolean isAdministrator;
    private Long passwordExpires;
    private String queryId;
    private String secretKey;
    private String temporaryPassword;

	// displayUserRecordPage relies on empty strings and isAdministrator set
    public UserInfoWeb() {
		userName = "";
		realName = "";
		emailAddress = "";
		bCryptedPassword = "";
		telephoneNumber = "";
		affiliation = "";
		projectDescription = "";
		projectPIName = "";
		isAdministrator = false;
	}

    public UserInfoWeb( String userName )
    {
        this.userName = userName;
    }

    // this sets all the mandatory fields in UserInfoWeb
    public UserInfoWeb( String userName, String realName, String emailAddress, String bCryptedPassword)
    {
        this.userName = userName;
        this.realName = realName;
        this.emailAddress = emailAddress;
        this.bCryptedPassword = bCryptedPassword;
        this.isAdministrator = false;
        this.isConfirmed = false;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return emailAddress;
    }

    public void setEmail(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getBCryptedPassword() {
        return bCryptedPassword;
    }

    public void setBCryptedPassword(String bCryptedPassword) {
        this.bCryptedPassword = bCryptedPassword;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String password) {
        this.temporaryPassword = password;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public String getProjectPIName() {
        return projectPIName;
    }

    public void setProjectPIName(String projectPIName) {
        this.projectPIName = projectPIName;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public String getCertificateCode() {
        return certificateCode;
    }

    public void setCertificateCode(String certificateCode) {
        this.certificateCode = certificateCode;
    }

  public Boolean isApproved()
  {
    return isApproved;
  }

  public void setIsApproved( Boolean approved )
  {
    isApproved = approved;
  }

  public Boolean isConfirmed()
  {
    return isConfirmed;
  }

  public void setIsConfirmed( Boolean confirmed )
  {
    isConfirmed = confirmed;
  }

  public Boolean isEnabled()
  {
    return isEnabled;
  }

  public void setIsEnabled( Boolean enabled )
  {
    isEnabled = enabled;
  }

  public Boolean isAdministrator()
  {
    return isAdministrator;
  }

  public void setIsAdministrator( Boolean administrator )
  {
    isAdministrator = administrator;
  }

    public Long getPasswordExpires()
    {
        return passwordExpires;
    }

    public void setPasswordExpires( Long passwordExpires )
    {
        this.passwordExpires = passwordExpires;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
