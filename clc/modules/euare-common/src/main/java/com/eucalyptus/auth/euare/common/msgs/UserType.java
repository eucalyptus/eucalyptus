/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.auth.euare.common.msgs;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class UserType extends EucalyptusData {

  private String path;
  private String userName;
  private String userId;
  private String arn;
  private Date createDate;
  private String enabled;
  private String passwordExpiration;

  public String getPath( ) {
    return path;
  }

  public void setPath( String path ) {
    this.path = path;
  }

  public String getUserName( ) {
    return userName;
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }

  public String getUserId( ) {
    return userId;
  }

  public void setUserId( String userId ) {
    this.userId = userId;
  }

  public String getArn( ) {
    return arn;
  }

  public void setArn( String arn ) {
    this.arn = arn;
  }

  public Date getCreateDate( ) {
    return createDate;
  }

  public void setCreateDate( Date createDate ) {
    this.createDate = createDate;
  }

  public String getEnabled( ) {
    return enabled;
  }

  public void setEnabled( String enabled ) {
    this.enabled = enabled;
  }

  public String getPasswordExpiration( ) {
    return passwordExpiration;
  }

  public void setPasswordExpiration( String passwordExpiration ) {
    this.passwordExpiration = passwordExpiration;
  }
}
