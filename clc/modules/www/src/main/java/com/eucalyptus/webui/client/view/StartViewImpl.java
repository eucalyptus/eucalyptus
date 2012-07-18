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

package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class StartViewImpl extends Composite implements StartView {
  
  private static StartViewImplUiBinder uiBinder = GWT.create( StartViewImplUiBinder.class );
  
  interface StartViewImplUiBinder extends UiBinder<Widget, StartViewImpl> {}
  
  @UiField
  SimplePanel serviceSnippet;

  @UiField
  SimplePanel iamSnippet;

  @UiField
  SimplePanel cloudRegSnippet;

  @UiField
  SimplePanel downloadSnippet;

  @UiField
  Anchor serviceHeader;

  @UiField
  Anchor iamHeader;

  @UiField
  Anchor cloudRegHeader;

  @UiField
  Anchor downloadHeader;

  
  public StartViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @UiHandler( "serviceHeader" )
  void handleServiceHeaderClick( ClickEvent e ) {
    serviceSnippet.setVisible( !serviceSnippet.isVisible( ) );
  }

  @UiHandler( "iamHeader" )
  void handleIamHeaderClick( ClickEvent e ) {
    iamSnippet.setVisible( !iamSnippet.isVisible( ) );
  }

  @UiHandler( "cloudRegHeader" )
  void handlecloudRegHeaderClick( ClickEvent e ) {
    cloudRegSnippet.setVisible( !cloudRegSnippet.isVisible( ) );
  }

  @UiHandler( "downloadHeader" )
  void handleDownloadHeaderClick( ClickEvent e ) {
    downloadSnippet.setVisible( !downloadSnippet.isVisible( ) );
  }

  @Override
  public AcceptsOneWidget getCloudRegSnippetDisplay( ) {
    return cloudRegSnippet;
  }

  @Override
  public AcceptsOneWidget getDownloadSnippetDisplay( ) {
    return downloadSnippet;
  }

  @Override
  public AcceptsOneWidget getServiceSnippetDisplay( ) {
    return serviceSnippet;
  }

  @Override
  public AcceptsOneWidget getIamSnippetDisplay( ) {
    return iamSnippet;
  }
  
}
