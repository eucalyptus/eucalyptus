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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 *
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.client;

import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.DatabaseWrappedUser;
import com.eucalyptus.util.Composite;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by IntelliJ IDEA.
 * User: dmitriizagorodnov
 * Date: May 3, 2008
 * Time: 3:35:41 PM
 * To change this template use File | Settings | File Templates.
 */
@Composite({UserInfo.class, DatabaseWrappedUser.class})
public class UserInfoWeb implements IsSerializable
{
    /** these come from com.eucalyptus.auth.UserInfo.class **/
    private String userName;
    private String realName;
    private String email;
    private String telephoneNumber;
    private String affiliation;
    private String projectDescription;
    private String projectPIName;
    private Boolean approved;
    private Boolean confirmed;
    private String confirmationCode;
    private Long passwordExpires;
    
    /** these come from com.eucalyptus.auth.UserEntity.class **/
    private Boolean enabled;
    private Boolean administrator;
    private String password;
    private String token;
    private String queryId;
    private String secretKey;

    public static String BOGUS_ENTRY = "n/a"; // mirrors value in UserInfo
    
	// displayUserRecordPage relies on empty strings and isAdministrator set
    public UserInfoWeb() {
      this.userName = "";
      this.realName = "";
      this.email = "";
      this.password = "";
      this.telephoneNumber = "";
      this.affiliation = "";
      this.projectDescription = "";
      this.projectPIName = "";
      this.administrator = false;
	}

    public UserInfoWeb( String userName )
    {
        this.userName = userName;
    }

    // this sets all the mandatory fields in UserInfoWeb
    public UserInfoWeb( String userName, String realName, String email, String password)
    {
        this.userName = userName;
        this.realName = realName;
        this.email = email;
        this.password = password;
        this.administrator = false;
        this.confirmed = false;
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
        return email;
    }

    public void setEmail(String emailAddress) {
        this.email = emailAddress;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String bCryptedPassword) {
        this.password = bCryptedPassword;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

  public Boolean isApproved()
  {
    return approved;
  }

  public void setApproved( Boolean approved )
  {
    this.approved = approved;
  }

  public Boolean isConfirmed()
  {
    return confirmed;
  }

  public void setConfirmed( Boolean confirmed )
  {
    this.confirmed = confirmed;
  }

  public Boolean isEnabled()
  {
    return enabled;
  }

  public void setEnabled( Boolean enabled )
  {
    this.enabled = enabled;
  }

  public Boolean isAdministrator()
  {
    return administrator;
  }

  public void setAdministrator( Boolean administrator )
  {
    this.administrator = administrator;
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
