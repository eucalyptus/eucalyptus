package com.eucalyptus.webui.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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
    String hint( );
    String eucaLabel( );
  }
  
  private static final String LOGINFORM_ID = "loginForm";
  private static final String LOGINFORM_USERNAME_ID = "userName";
  private static final String LOGINFORM_PASSWORD_ID = "password";
  private static final String LOGINFORM_STAYSIGNEDIN_ID = "checkStaySignedIn";
  private static final String LOGINFORM_SIGNINLABEL_ID = "signInLabel";
  private static final String LOGINFORM_EUCALABEL_ID = "eucaLabel";
  private static final String LOGINFORM_USERNAMELABEL_ID = "usernameLabel";
  private static final String LOGINFORM_FORMATLABEL_ID = "formatLabel";
  private static final String LOGINFORM_PASSWORDLABEL_ID = "passwordLabel";
  private static final String LOGINFORM_STAYSIGNEDINLABEL_ID = "staySignedInLabel";
  
  private Presenter listener;
  
  @UiField
  DivElement loginArea;
  
  @UiField
  LoginFormStyle formStyle;
  
  @UiField
  Label prompt;
  
  public LoginViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    injectLoginForm( );
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
    Document.get( ).getElementById( LOGINFORM_PASSWORDLABEL_ID ).addClassName( formStyle.loginLabel( ) );
    Document.get( ).getElementById( LOGINFORM_PASSWORD_ID ).addClassName( formStyle.loginInput( ) );
    Document.get( ).getElementById( LOGINFORM_STAYSIGNEDINLABEL_ID ).addClassName( formStyle.checkLabel( ) );
    Document.get( ).getElementById( LOGINFORM_FORMATLABEL_ID ).addClassName( formStyle.hint( ) );
    Document.get( ).getElementById( LOGINFORM_SIGNINLABEL_ID ).addClassName( formStyle.loginLabel( ) );
    Document.get( ).getElementById( LOGINFORM_EUCALABEL_ID ).addClassName( formStyle.eucaLabel( ) );
  }
  
  private native void injectLoginFormAction( LoginView view ) /*-{
    $wnd.__gwt_login = function() {
      view.@com.eucalyptus.webui.client.view.LoginViewImpl::login()();
    }
  }-*/;
  
  private void login( ) {
    String username = ( ( InputElement ) Document.get( ).getElementById( LOGINFORM_USERNAME_ID ) ).getValue( );
    String password = ( ( InputElement ) Document.get( ).getElementById( LOGINFORM_PASSWORD_ID ) ).getValue( );
    boolean staySignedIn = ( ( InputElement ) Document.get( ).getElementById( LOGINFORM_STAYSIGNEDIN_ID ) ).isChecked( );
    this.listener.login( username, password, staySignedIn );
  }
  
  @Override
  public void setPresenter( Presenter listener ) {
    this.listener = listener;
  }

  @Override
  public void setPrompt( String prompt ) {
    this.prompt.setText( prompt );
  }
  
}
