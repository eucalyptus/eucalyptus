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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.webui.client.session;

import java.util.ArrayList;
import java.util.HashMap;
import com.eucalyptus.webui.client.service.QuickLink;
import com.eucalyptus.webui.client.service.QuickLinkTag;
import com.eucalyptus.webui.client.service.LoginUserProfile;

public class SessionData {
  
  public static final String VERSION = "version";
  public static final String SEARCH_RESULT_PAGE_SIZE = "search-result-page-size";
  public static final String RIGHTSCALE_REGISTRATION_BASE_URL = "rightscale-registration-base-url";
  public static final String EUSTORE_DESCRIPTION = "eustore-description";
  
  private LoginUserProfile user;
  private HashMap<String, String> props = new HashMap<String, String>( );
  private ArrayList<QuickLinkTag> category = new ArrayList<QuickLinkTag>( );
  
  public SessionData( ) {
  }
  
  public LoginUserProfile getLoginUser( ) {
    return this.user;
  }
  
  public void setLoginUser( LoginUserProfile user ) {
    this.user = user;
  }

  public String getProperty( String name ) {
    return this.props.get( name );
  }

  public void setProperties( HashMap<String, String> input ) {
    this.props.putAll( input );
  }
  
  public int getIntProperty( String name, int def ) {
    try {
      String value = getProperty( name );
      if ( value == null ) {
        return def;
      }
      return Integer.parseInt( getProperty( name ) );
    } catch ( Exception e ) {
      return def;
    }
  }
  
  public String getStringProperty( String name, String def ) {
    String val = getProperty( name );
    return ( val == null ? def : val );
  }

  public void setQuickLinks( ArrayList<QuickLinkTag> category ) {
    this.category.addAll( category );
  }

  public ArrayList<QuickLinkTag> getQuickLinks( ) {
    return this.category;
  }
  
  public QuickLink lookupQuickLink( String fullSearch ) {
    if ( this.category != null ) {
      for ( QuickLinkTag tag : this.category ) {
        for ( QuickLink item : tag.getItems( ) ) {
          if ( item.getQuery( ) != null && item.getQuery( ).equals( fullSearch ) ) {
            return item;
          }
        }
      }
    }
    return null;
  }
  
}
