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

public class ListAttachedUserPoliciesType extends EuareMessage implements EuareMessageWithDelegate {

  private String delegateAccount;
  private String marker;
  private Integer maxItems;
  private String pathPrefix;
  private String userName;

  public String getDelegateAccount( ) {
    return delegateAccount;
  }

  public void setDelegateAccount( String delegateAccount ) {
    this.delegateAccount = delegateAccount;
  }

  public String getMarker( ) {
    return marker;
  }

  public void setMarker( String marker ) {
    this.marker = marker;
  }

  public Integer getMaxItems( ) {
    return maxItems;
  }

  public void setMaxItems( Integer maxItems ) {
    this.maxItems = maxItems;
  }

  public String getPathPrefix( ) {
    return pathPrefix;
  }

  public void setPathPrefix( String pathPrefix ) {
    this.pathPrefix = pathPrefix;
  }

  public String getUserName( ) {
    return userName;
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }
}
