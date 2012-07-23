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

package com.eucalyptus.webui.client.service;

import java.io.Serializable;

public class DownloadInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  private String url;
  private String name;
  private String description;
  
  public DownloadInfo( ) {
  }
  
  public DownloadInfo( String url, String name, String description ) {
    this.setUrl( url );
    this.setName( name );
    this.setDescription( description );
  }

  public void setUrl( String url ) {
    this.url = url;
  }

  public String getUrl( ) {
    return url;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getName( ) {
    return name;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getDescription( ) {
    return description;
  }
  
}
