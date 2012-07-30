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
import java.util.Set;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class ConfirmationViewImpl extends DialogBox implements ConfirmationView {
  
  private static final String COL_WIDTH = "100px";

  private static ConfirmationViewImplUiBinder uiBinder = GWT.create( ConfirmationViewImplUiBinder.class );
  
  interface ConfirmationViewImplUiBinder extends UiBinder<Widget, ConfirmationViewImpl> {}

  interface GridStyle extends CssResource {
    String grid( );
    String html( );
  }
  
  @UiField
  Label subject;
  
  @UiField
  ScrollPanel contentPanel;
  
  @UiField
  GridStyle gridStyle;
  
  private Presenter presenter;
  
  public ConfirmationViewImpl( ) {
    super( );
    setWidget( uiBinder.createAndBindUi( this ) );
    setGlassEnabled( true );
  }
  
  @UiHandler( "ok" )
  void handleOkClickEvent( ClickEvent event ) {
    hide( );
    this.presenter.confirm( subject.getText( ) );
  }

  @UiHandler( "cancel" )
  void handleCancelClickEvent( ClickEvent event ) {
    hide( );
  }

  @Override
  public void display( String caption, String subject, Set<SearchResultRow> list, ArrayList<Integer> fields ) {
    this.setText( caption );
    contentPanel.clear( );
    this.subject.setText( subject );
    
    if ( list.size( ) > 0 && fields.size( ) > 0 ) {
      Grid grid = new Grid( list.size( ), fields.size( ) );
      grid.addStyleName( gridStyle.grid( ) );
      grid.getColumnFormatter( ).setWidth( 0, COL_WIDTH );
      grid.getColumnFormatter( ).setWidth( 1, COL_WIDTH );
      int row = 0;
      for ( SearchResultRow rowData : list ) {
        int col = 0;
        for ( Integer field : fields ) {
          String text = rowData.getField( field );
          grid.setText( row, col++, text == null ? "" : text );
        }
        row++;
      }
      contentPanel.setWidget( grid );
    }
    center( );
    show( );
  }

  @Override
  public void display( String caption, String subject, SafeHtml html ) {
    this.setText( caption );
    contentPanel.clear( );
    this.subject.setText( subject );
    
    HTML widget = new HTML( html );
    widget.setStyleName( gridStyle.html( ) );
    contentPanel.setWidget( widget );
    
    center( );
    show( );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }
  
}
