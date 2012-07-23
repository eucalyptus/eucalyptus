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

public class GuideItem implements Serializable {

  private static final long serialVersionUID = 1L;

  private String title;
  private String link;
  private String icon;
  
  public GuideItem( ) {
  }
  
  public GuideItem( String title, String link, String icon ) {
    this.setTitle( title );
    this.setLink( link );
    this.setIcon( icon );
  }

  public void setTitle( String title ) {
    this.title = title;
  }

  public String getTitle( ) {
    return title;
  }

  public void setLink( String link ) {
    this.link = link;
  }

  public String getLink( ) {
    return link;
  }

  public void setIcon( String icon ) {
    this.icon = icon;
  }

  public String getIcon( ) {
    return icon;
  }
  
}
