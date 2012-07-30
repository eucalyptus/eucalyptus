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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class LoginViewImpl extends Composite implements LoginView {
  
  private static LoginViewImplUiBinder uiBinder = GWT.create( LoginViewImplUiBinder.class );
  interface LoginViewImplUiBinder extends UiBinder<Widget, LoginViewImpl> { }
  
  interface LoginFormStyle extends CssResource {
    String loginBox( );
    String loginLabel( );
    String loginInput( );
    String checkLabel( );
    String eucaLabel( );
  }
  
  private static final String LOGINFORM_ID = "loginForm";
  private static final String LOGINFORM_ACCOUNTNAME_ID = "accountName";
  private static final String LOGINFORM_USERNAME_ID = "userName";
  private static final String LOGINFORM_PASSWORD_ID = "password";
  private static final String LOGINFORM_STAYSIGNEDIN_ID = "checkStaySignedIn";
  private static final String LOGINFORM_SIGNINLABEL_ID = "signInLabel";
  private static final String LOGINFORM_EUCALABEL_ID = "eucaLabel";
  private static final String LOGINFORM_ACCOUNTNAMELABEL_ID = "accountnameLabel";
  private static final String LOGINFORM_USERNAMELABEL_ID = "usernameLabel";
  private static final String LOGINFORM_PASSWORDLABEL_ID = "passwordLabel";
  private static final String LOGINFORM_STAYSIGNEDINLABEL_ID = "staySignedInLabel";
  
  private Presenter presenter;
  
  @UiField
  DivElement loginArea;
  
  @UiField
  LoginFormStyle formStyle;
  
  @UiField
  Label prompt;
  
  @UiField
  Anchor userSignup;
  
  public LoginViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    injectLoginForm( );
    // temporarily disable user signup based on support team's suggestions.
    // TODO(wenye): really remove it or recover it.
    userSignup.setVisible( false );
  }

  @UiHandler( "accountSignup" )
  void handleAccountSignupButtonClick( ClickEvent e ) {
    this.presenter.onAccountSignup( );
  }

  @UiHandler( "userSignup" )
  void handleUserSignupButtonClick( ClickEvent e ) {
    this.presenter.onUserSignup( );
  }

  @UiHandler( "recover" )
  void handleRecoverButtonClick( ClickEvent e ) {
    this.presenter.onRecoverPassword( );
  }

  private void injectLoginForm( ) {
    // Inject style first. After the form is wrapped, it is too late (IDs are gone)
    injectLoginFormStyle( );
    FormPanel form = FormPanel.wrap( Document.get( ).getElementById( LOGINFORM_ID ), false );
    form.setAction( "javascript:__gwt_login()" );
    loginArea.appendChild( form.getElement( ) );
    injectLoginFormAction( this );
  }
  
  private void injectLoginFormStyle( ) {
    Document.get( ).getElementById( LOGINFORM_ID ).addClassName( formStyle.loginBox( ) );
    Document.get( ).getElementById( LOGINFORM_USERNAMELABEL_ID ).addClassName( formStyle.loginLabel( ) );
    Document.get( ).getElementById( LOGINFORM_USERNAME_ID ).addClassName( formStyle.loginInput( ) );
    Document.get( ).getElementById( LOGINFORM_ACCOUNTNAMELABEL_ID ).addClassName( formStyle.loginLabel( ) );
    Document.get( ).getElementById( LOGINFORM_ACCOUNTNAME_ID ).addClassName( formStyle.loginInput( ) );
    Document.get( ).getElementById( LOGINFORM_PASSWORDLABEL_ID ).addClassName( formStyle.loginLabel( ) );
    Document.get( ).getElementById( LOGINFORM_PASSWORD_ID ).addClassName( formStyle.loginInput( ) );
    Document.get( ).getElementById( LOGINFORM_STAYSIGNEDINLABEL_ID ).addClassName( formStyle.checkLabel( ) );
    Document.get( ).getElementById( LOGINFORM_SIGNINLABEL_ID ).addClassName( formStyle.loginLabel( ) );
    Document.get( ).getElementById( LOGINFORM_EUCALABEL_ID ).addClassName( formStyle.loginLabel( ) );
  }
  
  private native void injectLoginFormAction( LoginView view ) /*-{
    $wnd.__gwt_login = function() {
      view.@com.eucalyptus.webui.client.view.LoginViewImpl::login()();
    }
  }-*/;
  
  private void login( ) {
    String accountName = ( ( InputElement ) Document.get( ).getElementById( LOGINFORM_ACCOUNTNAME_ID ) ).getValue( );
    String userName = ( ( InputElement ) Document.get( ).getElementById( LOGINFORM_USERNAME_ID ) ).getValue( );
    String password = ( ( InputElement ) Document.get( ).getElementById( LOGINFORM_PASSWORD_ID ) ).getValue( );
    boolean staySignedIn = ( ( InputElement ) Document.get( ).getElementById( LOGINFORM_STAYSIGNEDIN_ID ) ).isChecked( );
    this.presenter.login( accountName, userName, password, staySignedIn );
  }
  
  @Override
  public void setPresenter( Presenter listener ) {
    this.presenter = listener;
  }

  @Override
  public void setPrompt( String prompt ) {
    this.prompt.setText( prompt );
  }

  @Override
  public void setFocus( ) {
    Document.get( ).getElementById( LOGINFORM_ACCOUNTNAME_ID ).focus( );
  }

  @Override
  public void clearPassword( ) {
    ( ( InputElement ) Document.get( ).getElementById( LOGINFORM_PASSWORD_ID ) ).setValue( "" );
  }
  
}
