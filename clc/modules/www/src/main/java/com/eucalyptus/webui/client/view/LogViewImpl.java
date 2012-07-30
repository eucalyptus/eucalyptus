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

import java.util.Date;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class LogViewImpl extends Composite implements LogView {
  
  private static LogViewImplUiBinder uiBinder = GWT.create( LogViewImplUiBinder.class );
  
  interface LogViewImplUiBinder extends UiBinder<Widget, LogViewImpl> {}
  
  interface GridStyle extends CssResource {
    String grid( );
  }
  
  interface Resources extends ClientBundle {
    @Source( "image/info_12x12_blue.png" )
    ImageResource info( );
    
    @Source( "image/x_alt_12x12_red.png" )
    ImageResource error( );
  }
  
  private static final int MAX_LOG_LINES = 1024;
  private static final String TIME_COL_WIDTH = "160px";
  private static final String ICON_COL_WIDTH = "30px";
  
  @UiField
  ScrollPanel panel;
  
  @UiField
  GridStyle gridStyle;
  
  private Resources resources;
  
  private Grid currentGrid;
  
  public LogViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    resources = GWT.create( Resources.class );
    createGrid( );
    this.panel.setWidget( currentGrid );
  }

  private void createGrid( ) {
    this.currentGrid = new Grid( 1, 3 );
    this.currentGrid.addStyleName( gridStyle.grid( ) );
    this.currentGrid.getColumnFormatter( ).setWidth( 0, TIME_COL_WIDTH );
    this.currentGrid.getColumnFormatter( ).setWidth( 1, ICON_COL_WIDTH );
    this.currentGrid.setText( 0, 0, getTimeString( new Date( ) ) );
    this.currentGrid.setWidget( 0, 1, getLogIcon( LogType.INFO ) );
    this.currentGrid.setText( 0, 2, "Main screen loaded" );
  }
  
  private String getTimeString( Date date ) {
    return DateTimeFormat.getFormat( DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM ).format( date );
  }

  private Image getLogIcon( LogType type ) {
    switch ( type ) {
      case ERROR:
        return new Image( resources.error( ) );
      default:
        return new Image( resources.info( ) );
    }
  }
  
  @Override
  public void log( LogType type, String content ) {
    this.currentGrid.insertRow( 0 );
    this.currentGrid.setText( 0, 0, getTimeString( new Date( ) ) );
    this.currentGrid.setWidget( 0, 1, getLogIcon( type ) );
    this.currentGrid.setText( 0, 2, content != null ? content : "" );
    truncateLog( );
  }

  private void truncateLog( ) {
    if ( this.currentGrid.getRowCount( ) > MAX_LOG_LINES ) {    
      this.currentGrid.removeRow( this.currentGrid.getRowCount( ) - 1 );
    }
  }

  @Override
  public void clear( ) {
    while ( this.currentGrid.getRowCount( ) > 0 ) {
      this.currentGrid.removeRow( 0 );
    }
  }
  
}
