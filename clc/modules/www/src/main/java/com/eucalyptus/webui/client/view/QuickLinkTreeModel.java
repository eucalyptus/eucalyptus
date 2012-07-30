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

package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.eucalyptus.webui.client.service.QuickLink;
import com.eucalyptus.webui.client.service.QuickLinkTag;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;

public class QuickLinkTreeModel implements TreeViewModel {
  
  /*
   * Tree icons
   */
  static interface Images extends ClientBundle {
    @Source( "image/document_stroke_12x12_gray.png" )
    ImageResource def( );
    @Source( "image/tag_fill_12x12_gray.png" )
    ImageResource tag( );
    @Source( "image/cog_alt_12x12_gray.png" )
    ImageResource config( );
    @Source( "image/user_12x16_gray.png" )
    ImageResource user( );
    @Source( "image/group_12x11_gray.png" )
    ImageResource group( );    
    @Source( "image/article_12x12_gray.png" )
    ImageResource report( );
    @Source( "image/home_12x12_gray.png" )
    ImageResource home( );
    @Source( "image/image_12x12_gray.png" )
    ImageResource image( );
    @Source( "image/tag_stroke_12x12_gray.png" )
    ImageResource type( );
    @Source( "image/arrow_down_12x12_gray.png" )
    ImageResource down( );
    @Source( "image/dollar_12x11_gray.png" )
    ImageResource dollar( );    
    @Source( "image/key_fill_12x12_gray.png" )
    ImageResource key( );    
    @Source( "image/lock_fill_12x16_gray.png" )
    ImageResource lock( );    
    @Source( "image/sun_12x12_gray.png" )
    ImageResource sun( );
    
  }
  
  private static class QuickLinkTagCell extends AbstractCell<QuickLinkTag> {

    public QuickLinkTagCell( ) {
    }
    
    @Override
    public void render( Context context, QuickLinkTag value, SafeHtmlBuilder sb ) {
      if ( value != null ) {
        sb.appendHtmlConstant( getImageHtml( images.tag( ).getName( ) ) )
          .appendEscaped( " " )
          .appendEscaped( value.getName( ) );
      }
    }
  }
  
  private static class QuickLinkCell extends AbstractCell<QuickLink> {
    
    public QuickLinkCell( ) {
    }
    
    @Override
    public void render( Context context, QuickLink value, SafeHtmlBuilder sb ) {
      if ( value != null ) {
        sb.appendHtmlConstant( getImageHtml( value.getImage( ) ) )
          .appendEscaped( " " )
          .appendEscaped( value.getName( ) );
      }
    }
  }
  
  private ArrayList<QuickLinkTag> tags;
  
  private SelectionModel<QuickLink> selectionModel;
  
  private static Images images;
  private static HashMap<String, String> imageMap = new HashMap<String, String>( );
  
  public QuickLinkTreeModel( ArrayList<QuickLinkTag> tags, SelectionModel<QuickLink> selectionModel ) {
    this.tags = tags;
    this.selectionModel = selectionModel;
    
    setupImageMap( );
  }
  
  private static void setupImageMap( ) {
    if ( images == null ) {
      images = GWT.create( Images.class );
      imageMap.put( images.def( ).getName( ), AbstractImagePrototype.create( images.def( ) ).getHTML( ) );
      imageMap.put( images.tag( ).getName( ), AbstractImagePrototype.create( images.tag( ) ).getHTML( ) );
      imageMap.put( images.config( ).getName( ), AbstractImagePrototype.create( images.config( ) ).getHTML( ) );
      imageMap.put( images.user( ).getName( ), AbstractImagePrototype.create( images.user( ) ).getHTML( ) );
      imageMap.put( images.report( ).getName( ), AbstractImagePrototype.create( images.report( ) ).getHTML( ) );
      imageMap.put( images.home( ).getName( ), AbstractImagePrototype.create( images.home( ) ).getHTML( ) );
      imageMap.put( images.image( ).getName( ), AbstractImagePrototype.create( images.image( ) ).getHTML( ) );
      imageMap.put( images.type( ).getName( ), AbstractImagePrototype.create( images.type( ) ).getHTML( ) );
      imageMap.put( images.group( ).getName( ), AbstractImagePrototype.create( images.group( ) ).getHTML( ) );
      imageMap.put( images.down( ).getName( ), AbstractImagePrototype.create( images.down( ) ).getHTML( ) );
      imageMap.put( images.dollar( ).getName( ), AbstractImagePrototype.create( images.dollar( ) ).getHTML( ) );
      imageMap.put( images.key( ).getName( ), AbstractImagePrototype.create( images.key( ) ).getHTML( ) );
      imageMap.put( images.lock( ).getName( ), AbstractImagePrototype.create( images.lock( ) ).getHTML( ) );
      imageMap.put( images.sun( ).getName( ), AbstractImagePrototype.create( images.sun( ) ).getHTML( ) );
    }    
  }
  
  private static String getImageHtml( String name ) {
    String imageHtml = "";
    if ( name != null ) {
      imageHtml = imageMap.get( name );
    }
    if ( imageHtml == null ) {
      imageHtml = imageMap.get( images.def( ).getName( ) );
    }
    return imageHtml;
  }
  
  @Override
  public <T> NodeInfo<?> getNodeInfo( T value ) {
    if ( value == null ) {
      // Level 0
      ListDataProvider<QuickLinkTag> dataProvider = new ListDataProvider<QuickLinkTag>( this.tags );
      return new DefaultNodeInfo<QuickLinkTag>( dataProvider, new QuickLinkTagCell( ) );
    } else if ( value instanceof QuickLinkTag ) {
      // Level 1
      ListDataProvider<QuickLink> dataProvider = new ListDataProvider<QuickLink>( ( ( QuickLinkTag ) value ).getItems( ) );
      return new DefaultNodeInfo<QuickLink>( dataProvider, new QuickLinkCell( ), selectionModel, null );
    }
    return null;
  }

  @Override
  public boolean isLeaf( Object value ) {
    if ( value != null && value instanceof QuickLink ) {
      return true;
    }
    return false;
  }
  
}
