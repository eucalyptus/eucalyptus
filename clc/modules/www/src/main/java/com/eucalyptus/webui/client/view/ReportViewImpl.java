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

import static com.google.gwt.i18n.client.DateTimeFormat.getFormat;
import static com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.eucalyptus.webui.client.ClientFactory;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.uibinder.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.datepicker.client.DateBox;


public class ReportViewImpl extends Composite implements ReportView {
  
  private static final Logger LOG = Logger.getLogger( ReportViewImpl.class.getName( ) );
  
  private static ReportViewImplUiBinder uiBinder = GWT.create( ReportViewImplUiBinder.class );
  
  interface ReportViewImplUiBinder extends UiBinder<Widget, ReportViewImpl> {}

  @UiField
  LayoutPanel contentPanel;
  
  @UiField
  DateBox fromDate;
  
  @UiField
  DateBox toDate;

  @UiField
  ListBox type;
  
  private Presenter presenter;
  private final ClientFactory clientFactory;

  private LoadingAnimationViewImpl loadingAnimation;
  private Frame iFrame;
    
  public ReportViewImpl( ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
    initWidget( uiBinder.createAndBindUi( this ) );
    loadingAnimation = new LoadingAnimationViewImpl( );
    iFrame = new Frame( );
    this.fromDate.setFormat( new DefaultFormat( getFormat( PredefinedFormat.DATE_LONG ) ) );
    this.toDate.setFormat( new DefaultFormat( getFormat( PredefinedFormat.DATE_LONG ) ) );
  }
  
  @UiHandler( "generateButton" )
  void handleGenerateButtonClick( ClickEvent e ) {
    final Date from = fromDate.getValue( );
    final Date to = toDate.getValue( );

    String errorMessage = null;
    if ( from == null || to == null ) {
      errorMessage = "Invalid report period.";
    } else if ( System.currentTimeMillis() < from.getTime() ) {
      errorMessage = "Invalid report period requested, 'From' must be in the past.";
    } else if ( from.getTime() > to.getTime() ) {
      errorMessage = "Invalid report period requested, 'From' must not be later than 'Through'";
    }

    if ( errorMessage != null ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus(
          FooterView.StatusType.ERROR, errorMessage, FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }

    this.contentPanel.clear( );
    this.contentPanel.add( loadingAnimation );
    this.presenter.generateReport( fromDate.getValue( ),
                                   toDate.getValue( ),
                                   type.getValue( type.getSelectedIndex( ) ) );
  }

  @UiHandler( "csvButton" )
  void handleCsvButtonClick( ClickEvent e ) {
    this.presenter.downloadCsv( );
  }

  @UiHandler( "htmlButton" )
  void handleHtmlButtonClick( ClickEvent e ) {
    this.presenter.downloadHtml( );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }

  @Override
  public void loadReport( String url ) {
    this.contentPanel.clear( );
    this.contentPanel.add( iFrame );
    this.iFrame.setUrl( url );
    iFrame.setWidth( "100%" );
    iFrame.setHeight( "100%" );
    LOG.log( Level.INFO, "Loading: " + url );
  }

  @Override
  public void init( Date fromDate, Date toDate, String[] typeList ) {
    initDate( this.fromDate, fromDate );
    initDate( this.toDate, toDate );
    initList( this.type, typeList );
  }

  private void initDate( DateBox w, Date date ) {
    if ( date != null ) {
      w.setValue( date );
    }
  }
  
  private void initList( ListBox l, String[] list ) {
    l.clear( );
    if ( list == null || list.length < 1 ) {
      l.addItem( "None" );
    } else {
      for ( final String aList : list ) {
        l.addItem( aList );
      }
    }
  }
}
