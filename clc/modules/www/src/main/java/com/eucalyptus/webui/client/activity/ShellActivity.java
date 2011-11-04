package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.AppWidget;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.ExPlaceHistoryHandler;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.LogoutPlace;
import com.eucalyptus.webui.client.place.ShellPlace;
import com.eucalyptus.webui.client.service.QuickLinkTag;
import com.eucalyptus.webui.client.service.LoginUserProfile;
import com.eucalyptus.webui.client.session.SessionData;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.DirectoryView;
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.HeaderView;
import com.eucalyptus.webui.client.view.InputField;
import com.eucalyptus.webui.client.view.InputView;
import com.eucalyptus.webui.client.view.LoadingProgressView;
import com.eucalyptus.webui.client.view.FooterView.StatusType;
import com.eucalyptus.webui.client.view.LogView.LogType;
import com.eucalyptus.webui.client.view.ShellView;
import com.eucalyptus.webui.client.view.UserSettingView;
import com.eucalyptus.webui.shared.checker.ValueChecker;
import com.eucalyptus.webui.shared.checker.ValueCheckerFactory;
import com.google.common.base.Strings;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Bootstrapping activity that brings up the main UI. Then it sets up and launches
 * the main activity manager.
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 *
 */
public class ShellActivity extends AbstractActivity
    implements FooterView.Presenter, UserSettingView.Presenter, DetailView.Controller, InputView.Presenter, DirectoryView.Presenter, HeaderView.Presenter {
  
  private static final Logger LOG = Logger.getLogger( ShellActivity.class.getName( ) );
  
  private static final String DEFAULT_VERSION = "Eucalyptus unknown version";
  private static final String DEFAULT_LOGO_TITLE = "EUCALYPTUS";
  private static final String DEFAULT_LOGO_SUBTITLE = "YOUR FRIENDLY CLOUD";
  
  public static final String CHANGE_PASSWORD_CAPTION = "Change password";
  public static final String FIRST_TIME_CAPTION = "Enter first time information";
  public static final String MANUAL_CHANGE_PASSWORD_SUBJECT = "Please enter new password:";
  public static final String FIRST_TIME_SUBJECT = "First time login. Please fill in the following information:";
  public static final String PASSWORD_EXPIRED_SUBJECT = "Password expired. Please change your password:";
  public static final String OLD_PASSWORD_INPUT_TITLE = "Old password";
  public static final String NEW_PASSWORD_INPUT_TITLE = "New password";
  public static final String NEW_PASSWORD2_INPUT_TITLE = "Type again";
  public static final String EMAIL_INPUT_TITLE = "Email";
  
  private ClientFactory clientFactory;
  private ShellPlace place;
  
  private AcceptsOneWidget container;
  
  private ArrayList<QuickLinkTag> quicklinks;
  
  public ShellActivity( ShellPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( final AcceptsOneWidget container, EventBus eventBus ) {
    this.container = container;
    loadLoadingProgressView( container );
    getLoginUserProfile( );
  }
  
  private void startMain( ) {
    loadShellView( this.container );

    // This is the root container of all main activities
    AppWidget appWidget = new AppWidget( this.clientFactory.getShellView( ).getContentView( ).getContentContainer( ) );    
    this.clientFactory.getMainActivityManager( ).setDisplay(appWidget);
    
    ExPlaceHistoryHandler placeHistoryHandler = ( ExPlaceHistoryHandler ) clientFactory.getMainPlaceHistoryHandler( );
    placeHistoryHandler.handleCurrentHistory( );
  }
  
  private void loadLoadingProgressView( AcceptsOneWidget container ) {
    LoadingProgressView loadingProgressView = this.clientFactory.getLoadingProgressView( );
    loadingProgressView.setProgress( 1 );
    container.setWidget( loadingProgressView );
  }
  
  private void loadShellView( AcceptsOneWidget container ) {
    ShellView shellView = this.clientFactory.getShellView( );
    
    shellView.getDirectoryView( ).buildTree( this.quicklinks );
    shellView.getDirectoryView( ).setPresenter( this );
    
    shellView.getFooterView( ).setPresenter( this );
    shellView.getFooterView( ).setVersion( clientFactory.getSessionData( ).getStringProperty( SessionData.VERSION, DEFAULT_VERSION ) );
    
    String user = clientFactory.getSessionData( ).getLoginUser( ).toString( );
    shellView.getHeaderView( ).setUser( user );
    shellView.getHeaderView( ).getUserSetting( ).setUser( user );
    shellView.getHeaderView( ).getUserSetting( ).setPresenter( this );
    shellView.getHeaderView( ).setPresenter( this );
    
    shellView.getDetailView( ).setController( this );
    
    container.setWidget( shellView );
    
    shellView.getLogView( ).log( LogType.INFO, "Logged in as " + user );
  }
  
  private void getLoginUserProfile( ) {
    this.clientFactory.getBackendService( ).getLoginUserProfile( this.clientFactory.getLocalSession( ).getSession( ),
                                                               new AsyncCallback<LoginUserProfile>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Cannot get login user profile. Maybe session is invalid: " + caught );
        clientFactory.getLocalSession( ).clearSession( );
        clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );
      }
      
      @Override
      public void onSuccess( LoginUserProfile result ) {
        if ( result == null ) {
          LOG.log( Level.WARNING, "Got empty user profile" );
          clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );
        } else {
          clientFactory.getSessionData( ).setLoginUser( result );
          clientFactory.getLoadingProgressView( ).setProgress( 33 );
          if ( result.getLoginAction( ) != null ) {
            switch ( result.getLoginAction( ) ) {
              case FIRSTTIME:
                showFirstTimeDialog( );
                break;
              case EXPIRATION:
                showChangePasswordDialog( PASSWORD_EXPIRED_SUBJECT );
                break;
            }
          } else {
            getSystemProperties( );
          }
        }
      }
      
    });
  }
  
  private void getSystemProperties( ) {
    this.clientFactory.getBackendService( ).getSystemProperties( this.clientFactory.getLocalSession( ).getSession( ),
                                                                 new AsyncCallback<HashMap<String, String>>( ) {
      
      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Cannot get system properties: " + caught );
        clientFactory.getLocalSession( ).clearSession( );
        clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );
      }
      
      @Override
      public void onSuccess( HashMap<String, String> result ) {
        if ( result == null ) {
          LOG.log( Level.WARNING, "Got empty system properties" );
          clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );          
        } else {
          clientFactory.getSessionData( ).setProperties( result );
          clientFactory.getLoadingProgressView( ).setProgress( 67 );
          getCategory( );
        }
      }
    } );
  }
  
  private void getCategory( ) {
    this.clientFactory.getBackendService( ).getQuickLinks( this.clientFactory.getLocalSession( ).getSession( ),
                                                         new AsyncCallback<ArrayList<QuickLinkTag>>( ) {
      
      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Cannot get category: " + caught );
        clientFactory.getLocalSession( ).clearSession( );
        clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );
      }
      
      @Override
      public void onSuccess( ArrayList<QuickLinkTag> result ) {
        if ( result == null ) {
          LOG.log( Level.WARNING, "Got empty category" );
          clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.LOADING_FAILURE_PROMPT ) );          
        } else {
          quicklinks = result;
          clientFactory.getSessionData( ).setQuickLinks( result );
          clientFactory.getLoadingProgressView( ).setProgress( 100 );
          startMain( );
        }
      }
    } );
  }

  @Override
  public void onShowLogConsole( ) {
    this.clientFactory.getShellView( ).showLogConsole( );
  }
  
  @Override
  public void onHideLogConsole( ) {
    this.clientFactory.getShellView( ).hideLogConsole( );
  }

  @Override
  public void logout( ) {
    this.clientFactory.getBackendService( ).logout( this.clientFactory.getLocalSession( ).getSession( ), new AsyncCallback<Void>( ) {
      @Override
      public void onFailure( Throwable arg0 ) {
        // Don't care about failure.
      }
      @Override
      public void onSuccess( Void arg0 ) {
        LOG.log( Level.INFO, "User signed out." );
      }
    } );
    this.clientFactory.getLocalSession( ).clearSession( );
    this.clientFactory.getShellView( ).getLogView( ).clear( );
    this.clientFactory.getMainPlaceController( ).goTo( new LogoutPlace( ) );
  }

  public void search( String search ) {
    if ( search != null ) {
      LOG.log( Level.INFO, "New search: " + search );
      this.clientFactory.getMainHistorian( ).newItem( search, true/*issueEvent*/ );
    } else {
      LOG.log( Level.INFO, "Empty search!" );
    }
  }

  @Override
  public void hideDetail( ) {
    this.clientFactory.getShellView( ).hideDetail( );
  }

  // Clicked on user setting menu to show user profile
  @Override
  public void onShowProfile( ) {
    this.search( clientFactory.getSessionData( ).getLoginUser( ).getUserProfileSearch( ) );
  }

  private void showChangePasswordDialog( String subject ) {
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( CHANGE_PASSWORD_CAPTION, subject, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return OLD_PASSWORD_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.PASSWORD;
      }

      @Override
      public ValueChecker getChecker( ) {
        return null;
      }
      
    }, new InputField( ) {

      @Override
      public String getTitle( ) {
        return NEW_PASSWORD_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.NEWPASSWORD;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createPasswordChecker( );
      }
      
    }, new InputField( ) {

      @Override
      public String getTitle( ) {
        return NEW_PASSWORD2_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.PASSWORD;
      }

      @Override
      public ValueChecker getChecker( ) {
        return null;
      }
      
    } ) ) );
    
  }
  
  private void showFirstTimeDialog( ) {
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( FIRST_TIME_CAPTION, FIRST_TIME_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return EMAIL_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createEmailChecker( );
      }
      
    }, new InputField( ) {

      @Override
      public String getTitle( ) {
        return OLD_PASSWORD_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.PASSWORD;
      }

      @Override
      public ValueChecker getChecker( ) {
        return null;
      }
      
    }, new InputField( ) {

      @Override
      public String getTitle( ) {
        return NEW_PASSWORD_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.NEWPASSWORD;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createPasswordChecker( );
      }
      
    }, new InputField( ) {

      @Override
      public String getTitle( ) {
        return NEW_PASSWORD2_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.PASSWORD;
      }

      @Override
      public ValueChecker getChecker( ) {
        return null;
      }
      
    } ) ) );
    
  }
  
  // This is when user clicks the user setting manual to change password.
  @Override
  public void onChangePassword( ) {
    showChangePasswordDialog( MANUAL_CHANGE_PASSWORD_SUBJECT );
  }

  // Returned from dialog
  @Override
  public void process( String subject, ArrayList<String> values ) {
    if ( MANUAL_CHANGE_PASSWORD_SUBJECT.equals( subject ) || PASSWORD_EXPIRED_SUBJECT.equals( subject ) ) {
      doChangePassword( subject, values.get( 0 ), values.get( 1 ), null );
    } else if ( FIRST_TIME_SUBJECT.equals( subject ) ) {
      doChangePassword( subject, values.get( 1 ), values.get( 2 ), values.get( 0 ) );
    }
  }

  private void doChangePassword( final String subject, String oldPass, String newPass, String email ) {
    final String userId = this.clientFactory.getSessionData( ).getLoginUser( ).getUserId( );
    
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Changing password ...", 0 );
    
    this.clientFactory.getBackendService( ).changePassword( this.clientFactory.getLocalSession( ).getSession( ), userId, oldPass, newPass, email, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to change password", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to change password for user " + userId + ": " + caught.getMessage( ) );
        // Password change failure is the same as cancelling the dialog
        cancel( subject );
      }

      @Override
      public void onSuccess( Void arg ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Password changed", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Password changed for user " + userId );
        clientFactory.getSessionData( ).getLoginUser( ).setLoginAction( null );
        if ( FIRST_TIME_SUBJECT.equals( subject ) || PASSWORD_EXPIRED_SUBJECT.equals( subject ) ) {
          // If it is a forced password change, continue the loading.
          getSystemProperties( );
        }
      }
      
    } );
  }

  // This is when the password change dialog (forced or manual) being cancelled
  @Override
  public void cancel( String subject ) {
    if ( FIRST_TIME_SUBJECT.equals( subject ) || PASSWORD_EXPIRED_SUBJECT.equals( subject ) ) {
      // If user chooses not to change password this time, logout.
      LOG.log( Level.WARNING, "User chooses not to change password" );
      clientFactory.getLocalSession( ).clearSession( );
      clientFactory.getLifecyclePlaceController( ).goTo( new LoginPlace( LoginPlace.DEFAULT_PROMPT ) );
    }
    
  }
  
  @Override
  public void onDownloadCredential( ) {
    final LoginUserProfile user = clientFactory.getSessionData( ).getLoginUser( );
    this.clientFactory.getBackendService( ).getUserToken( this.clientFactory.getLocalSession( ).getSession( ), new AsyncCallback<String>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to initiate credential download", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to initiate credential download: " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( final String code ) {
        if ( Strings.isNullOrEmpty( code ) ) {
          clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to initiate credential download", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
          clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to initiate credential download: can't find user security code" );          
        } else {
          Window.open( GWT.getModuleBaseURL( ) + "getX509?" +
                       "account=" + user.getAccountName( ) +
                       "&user=" + user.getUserName( ) +
                       "&code=" + code,
                       "_self", "" );
        }
      }
      
    } );
    
  }

  @Override
  public void onShowKey( ) {
    this.search( clientFactory.getSessionData( ).getLoginUser( ).getUserKeySearch( ) );
  }

  @Override
  public void runManualSearch( String search ) {
    this.search( search );
  }

  @Override
  public void switchQuickLink( String search ) {
    // If we are already doing this search, do nothing.
    // This happens when we enter a new search by url or by manual search,
    // and the new search matches a quick link. The quick link will be selected
    // programatically. In this case, don't need to change search anymore.
    if ( search != null ) {
      String currentSearch = ActivityUtil.getCurrentSearch( clientFactory );
      LOG.info( "Switching quick link: search = " + search + ", current = " + currentSearch );
      if ( search.equals( currentSearch ) ) {
        return;
      }
    }
    this.search( search );
  }

  
}
