/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.webui.server;

public class WebSession {
  
  private String id;
  private String userId;
  private long creationTime;
  private long lastAccessTime;
  
  public WebSession( String id, String userId, long creationTime, long lastAccessTime ) {
    this.setId( id );
    this.setUserId( userId );
    this.setCreationTime( creationTime );
    this.setLastAccessTime( lastAccessTime );
  }

  public void setId( String id ) {
    this.id = id;
  }

  public String getId( ) {
    return id;
  }

  public void setCreationTime( long creationTime ) {
    this.creationTime = creationTime;
  }

  public long getCreationTime( ) {
    return creationTime;
  }

  public void setLastAccessTime( long lastAccessTime ) {
    this.lastAccessTime = lastAccessTime;
  }

  public long getLastAccessTime( ) {
    return lastAccessTime;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }
  
}
