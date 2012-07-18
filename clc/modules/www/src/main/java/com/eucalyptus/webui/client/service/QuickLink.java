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
import com.google.gwt.view.client.ProvidesKey;

public class QuickLink implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final ProvidesKey<QuickLink> KEY_PROVIDER = new ProvidesKey<QuickLink>( ) {

    @Override
    public Object getKey( QuickLink item ) {
      if ( item != null ) {
        return item.getName( );
      }
      return null;
    }
    
  };
  
  private String name;
  private String desc;
  private String image;
  private String query;
  
  public QuickLink( ) {
  }
  
  public QuickLink( String name, String desc, String image, String query ) {
    this.setName( name );
    this.setDesc( desc );
    this.setImage( image );
    this.setQuery( query );
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getName( ) {
    return name;
  }

  public void setDesc( String desc ) {
    this.desc = desc;
  }

  public String getDesc( ) {
    return desc;
  }

  public void setImage( String image ) {
    this.image = image;
  }

  public String getImage( ) {
    return image;
  }

  public void setQuery( String query ) {
    this.query = query;
  }

  public String getQuery( ) {
    return query;
  }
  
}
