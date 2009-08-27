/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
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
