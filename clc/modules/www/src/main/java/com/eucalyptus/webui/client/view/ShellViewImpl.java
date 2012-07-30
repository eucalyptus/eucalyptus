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

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;

public class ShellViewImpl extends Composite implements ShellView {
  
  private static final Logger LOG = Logger.getLogger( ShellViewImpl.class.getName( ) );
  
  private static ShellViewImplUiBinder uiBinder = GWT.create( ShellViewImplUiBinder.class );
  interface ShellViewImplUiBinder extends UiBinder<Widget, ShellViewImpl> {}
  
  interface ShellStyle extends CssResource {
    String left( );
    String right( );
  }
  
  private static final int ANIMATE_DURATION = 200;//ms
  
  public static final int DIRECTORY_WIDTH = 240;//px
  public static final int LOG_HEIGHT = 160;//px
  
  @UiField
  HeaderViewImpl header;
  
  @UiField
  DirectoryViewImpl directory;
  
  @UiField
  DetailViewImpl detail;
  
  @UiField
  FooterViewImpl footer;
  
  @UiField
  Anchor splitter;
  
  @UiField
  LogViewImpl log;

  @UiField
  ContentViewImpl content;
  
  @UiField
  ShellStyle shellStyle;
  
  private boolean directoryHidden = false;
  
  public ShellViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  @UiHandler( "splitter" )
  void handleSplitterClick( ClickEvent e ) {
    directoryHidden = !directoryHidden;
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    if ( directoryHidden ) {
      parent.setWidgetSize( directory, 0 );
      splitter.removeStyleName( shellStyle.left( ) );
      splitter.addStyleName( shellStyle.right( ) );
    } else {
      parent.setWidgetSize( directory, DIRECTORY_WIDTH );
      splitter.removeStyleName( shellStyle.right( ) );
      splitter.addStyleName( shellStyle.left( ) );
    }
    parent.animate( ANIMATE_DURATION );
  }
  
  @Override
  public void showDetail( int width ) {
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    parent.setWidgetSize( detail, width );
  }

  @Override
  public void hideDetail( ) {
    LOG.log( Level.INFO, "Hiding detail pane." );
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    parent.setWidgetSize( detail, 0 );
  }

  @Override
  public DirectoryView getDirectoryView( ) {
    return this.directory;
  }

  @Override
  public ContentView getContentView( ) {
    return this.content;
  }

  @Override
  public FooterView getFooterView( ) {
    return this.footer;
  }

  @Override
  public HeaderView getHeaderView( ) {
    return this.header;
  }
  
  @Override
  public void showLogConsole( ) {
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    parent.setWidgetSize( this.log, LOG_HEIGHT );
    parent.animate( ANIMATE_DURATION );
  }

  @Override
  public void hideLogConsole( ) {
    DockLayoutPanel parent = (DockLayoutPanel) this.getWidget( );
    parent.setWidgetSize( this.log, 0 );
    parent.animate( ANIMATE_DURATION );
  }

  @Override
  public DetailView getDetailView( ) {
    return this.detail;
  }

  @Override
  public LogView getLogView( ) {
    return this.log;
  }

}
