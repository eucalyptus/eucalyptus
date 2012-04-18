package com.eucalyptus.webui.client.activity;

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.ConfirmSignupPlace;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.view.ActionResultView.ResultType;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ConfirmSignupActivity extends AbstractActionActivity {
  
  public static final String CONFIRM_FAILURE_MESSAGE = "Can not confirm your signup. Please contact system administrator.";
  public static final String CONFIRM_SUCCESS_MESSAGE = "Your account is confirmed. You can proceed to login.";
  
  public ConfirmSignupActivity( ConfirmSignupPlace place, ClientFactory clientFactory ) {
    super( place, clientFactory );
  }

  @Override
  protected void doAction( String confirmationCode ) {
    clientFactory.getActionResultView( ).loading( );
    
    clientFactory.getBackendService( ).confirmUser( confirmationCode,  new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        clientFactory.getActionResultView( ).display( ResultType.ERROR, CONFIRM_FAILURE_MESSAGE, true );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getActionResultView( ).display( ResultType.INFO, CONFIRM_SUCCESS_MESSAGE, true );
      }
      
    } );
  }
  
  @Override
  public void onConfirmed( ) {
    // Make sure we don't bring the confirm action URL into main interface.
    History.newItem( "" );
    clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.DEFAULT_PROMPT ) );
  }
  
}
