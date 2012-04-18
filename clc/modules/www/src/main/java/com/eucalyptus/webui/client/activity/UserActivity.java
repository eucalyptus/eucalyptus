package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.SearchPlace;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.view.ConfirmationView;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.InputField;
import com.eucalyptus.webui.client.view.InputView;
import com.eucalyptus.webui.client.view.UserView;
import com.eucalyptus.webui.client.view.HasValueWidget;
import com.eucalyptus.webui.client.view.FooterView.StatusType;
import com.eucalyptus.webui.client.view.LogView.LogType;
import com.eucalyptus.webui.shared.checker.ValueChecker;
import com.eucalyptus.webui.shared.checker.ValueCheckerFactory;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class UserActivity extends AbstractSearchActivity
    implements UserView.Presenter, ConfirmationView.Presenter, InputView.Presenter {
  
  public static final String TITLE = "USERS";
  
  public static final String DELETE_USERS_CAPTION = "Delete selected users";
  public static final String DELETE_USERS_SUBJECT = "Are you sure you want to delete following selected users?";

  public static final String ADD_KEY_CAPTION = "Add access key for the selected user";
  public static final String ADD_KEY_SUBJECT = "Are you sure to add key for the following selected user?";
  
  public static final String ADD_GROUPS_CAPTION = "Add selected users to groups";
  public static final String ADD_GROUPS_SUBJECT = "Enter groups to add the selected users (using space to separate names):";
  public static final String GROUP_NAMES_INPUT_TITLE = "Group names";

  public static final String REMOVE_GROUPS_CAPTION = "Remove users from selected groups";
  public static final String REMOVE_GROUPS_SUBJECT = "Enter groups to remove the selected users (using space to separate names):";

  public static final String ADD_POLICY_CAPTION = "Add new policy";
  public static final String ADD_POLICY_SUBJECT = "Enter new policy to assign to the selected user:";
  public static final String POLICY_NAME_INPUT_TITLE = "Policy name";
  public static final String POLICY_CONTENT_INPUT_TITLE = "Policy content";

  public static final String ADD_CERT_CAPTION = "Add new certificate";
  public static final String ADD_CERT_SUBJECT = "Enter new certificate to assign to the selected user:";
  public static final String CERT_PEM_INPUT_TITLE = "Certificate (PEM)";
  
  public static final String CHANGE_PASSWORD_CAPTION = "Change password";
  public static final String CHANGE_PASSWORD_SUBJECT = "Enter new password:";
  public static final String OLD_PASSWORD_INPUT_TITLE = "Your current password";
  public static final String NEW_PASSWORD_INPUT_TITLE = "New user password";
  public static final String NEW_PASSWORD2_INPUT_TITLE = "New password again";
  
  public static final String APPROVE_USERS_CAPTION = "Approve selected users";
  public static final String APPROVE_USERS_SUBJECT = "Are you sure you want to approve following selected users?";

  public static final String REJECT_USERS_CAPTION = "Reject selected users";
  public static final String REJECT_USERS_SUBJECT = "Are you sure you want to reject following selected users?";

  private static final String ACTION_PASSWORD = "password";
  
  private static final Logger LOG = Logger.getLogger( UserActivity.class.getName( ) );
  
  private Set<SearchResultRow> currentSelected;
  
  public UserActivity( SearchPlace place, ClientFactory clientFactory ) {
    super( place, clientFactory );
  }

  @Override
  protected void doSearch( String query, SearchRange range ) {
    this.clientFactory.getBackendService( ).lookupUser( this.clientFactory.getLocalSession( ).getSession( ), search, range,
                                                           new AsyncCallback<SearchResult>( ) {
      
      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        LOG.log( Level.WARNING, "Search failed: " + caught );
        displayData( null );
      }
      
      @Override
      public void onSuccess( SearchResult result ) {
        LOG.log( Level.INFO, "Search success:" + result );
        displayData( result );
      }
      
    } );
  }
  
  @Override
  public void onSelectionChange( Set<SearchResultRow> selection ) {
    this.currentSelected = selection;
    if ( selection == null || selection.size( ) != 1 ) {
      LOG.log( Level.INFO, "Not a single selection" );      
      this.clientFactory.getShellView( ).hideDetail( );
    } else {
      LOG.log( Level.INFO, "Selection changed to " + selection );
      this.clientFactory.getShellView( ).showDetail( DETAIL_PANE_SIZE );
      showSingleSelectedDetails( selection.toArray( new SearchResultRow[0] )[0] );
    }
  }

  @Override
  public void saveValue( ArrayList<String> keys, ArrayList<HasValueWidget> values ) {
    final ArrayList<String> newVals = Lists.newArrayList( );
    for ( HasValueWidget w : values ) {
      newVals.add( w.getValue( ) );
    }
    
    final String userId = emptyForNull( getField( newVals, 0 ) );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Modifying user " + userId + " ...", 0 );
    
    clientFactory.getBackendService( ).modifyUser( clientFactory.getLocalSession( ).getSession( ), keys, newVals, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to modify user", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to modify user " + userId  + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Successfully modified user", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Modified user " + userId );
        clientFactory.getShellView( ).getDetailView( ).disableSave( );
        reloadCurrentRange( );
      }
      
    } );
  }

  @Override
  protected String getTitle( ) {
    return TITLE;
  }

  @Override
  protected void showView( SearchResult result ) {
    if ( this.view == null ) {
      this.view = this.clientFactory.getUserView( );
      ( ( UserView ) this.view ).setPresenter( this );
      container.setWidget( this.view );
      ( ( UserView ) this.view ).clear( );
    }
    ( ( UserView ) this.view ).showSearchResult( result );    
  }

  @Override
  public void onDeleteUsers( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Select users to delete", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    ConfirmationView dialog = this.clientFactory.getConfirmationView( );
    dialog.setPresenter( this );
    dialog.display( DELETE_USERS_CAPTION, DELETE_USERS_SUBJECT, currentSelected, new ArrayList<Integer>( Arrays.asList( 0, 1 ) ) );
  }

  @Override
  public void onAddGroups( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select users to add to groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( ADD_GROUPS_CAPTION, ADD_GROUPS_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return GROUP_NAMES_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createUserAndGroupNamesChecker( );
      }
      
    } ) ) );    
  }

  @Override
  public void onRemoveGroups( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select users to remove from groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( REMOVE_GROUPS_CAPTION, REMOVE_GROUPS_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return GROUP_NAMES_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createUserAndGroupNamesChecker( );
      }
      
    } ) ) );
  }

  @Override
  public void onAddPolicy( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select a single user to add policy", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( ADD_POLICY_CAPTION, ADD_POLICY_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return POLICY_NAME_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createPolicyNameChecker( );
      }
      
    }, new InputField( ) {

      @Override
      public String getTitle( ) {
        return POLICY_CONTENT_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXTAREA;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createNonEmptyValueChecker( );
      }
      
    } ) ) );
  }

  @Override
  public void onAddKey( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Select one user to add access key", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    ConfirmationView dialog = this.clientFactory.getConfirmationView( );
    dialog.setPresenter( this );
    dialog.display( ADD_KEY_CAPTION, ADD_KEY_SUBJECT, currentSelected, new ArrayList<Integer>( Arrays.asList( 0, 1 ) ) );
  }

  @Override
  public void onAddCert( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select a single user to add certificate", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( ADD_CERT_CAPTION, ADD_CERT_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return CERT_PEM_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXTAREA;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createNonEmptyValueChecker( );
      }
      
    } ) ) );
  }

  @Override
  public void process( String subject, ArrayList<String> values ) {
    if ( ADD_GROUPS_SUBJECT.equals( subject ) ) {
      doAddGroups( values.get( 0 ) );
    } else if ( REMOVE_GROUPS_SUBJECT.equals( subject ) ) {
      doRemoveGroups( values.get( 0 ) );
    } else if ( ADD_POLICY_SUBJECT.equals( subject ) ) {
      doAddPolicy( values.get( 0 ), values.get( 1 ) );
    } else if ( ADD_CERT_SUBJECT.equals( subject ) ) {
      doAddCert( values.get( 0 ) );
    } else if ( CHANGE_PASSWORD_SUBJECT.equals( subject ) ) {
      doChangePassword( values.get( 0 ), values.get( 1 ) );
    }
  }

  private void doAddGroups( final String names ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      return;
    }
    // Selected user IDs
    final ArrayList<String> ids = Lists.newArrayList( ); 
    for ( SearchResultRow row : currentSelected ) {
      ids.add( row.getField( 0 ) );
    }
    
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Adding selected users to groups ...", 0 );
    
    clientFactory.getBackendService( ).addUsersToGroupsById( clientFactory.getLocalSession( ).getSession( ), ids, names, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to add selected users to groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to add users " + ids + " to groups " + names + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Selected users were added to groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Users " + ids + " are added to groups " + names );
      }
      
    } );
  }

  private void doRemoveGroups( final String names ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      return;
    }
    // Selected user IDs
    final ArrayList<String> ids = Lists.newArrayList( ); 
    for ( SearchResultRow row : currentSelected ) {
      ids.add( row.getField( 0 ) );
    }
    
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Removing selected users from groups ...", 0 );
    
    clientFactory.getBackendService( ).removeUsersFromGroupsById( clientFactory.getLocalSession( ).getSession( ), ids, names, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to remove selected users from groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to remove users " + ids + " from groups " + names + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Selected users were removed from groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Users " + ids + " are removed from groups " + names );
      }
      
    } );
  }

  private void doAddPolicy( final String name, final String document ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      return;
    }
    final String userId = this.currentSelected.toArray( new SearchResultRow[0] )[0].getField( 0 );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Adding policy " + name + " ...", 0 );
    
    this.clientFactory.getBackendService( ).addUserPolicy( this.clientFactory.getLocalSession( ).getSession( ), userId, name, document, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to add policy", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to add policy " + name + " for user " + userId + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Policy added", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "New policy " + name + " is added to user " + userId );
        //reloadCurrentRange( );
      }
      
    } );
  }

  private void doAddCert( final String pem ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      return;
    }
    final String userId = this.currentSelected.toArray( new SearchResultRow[0] )[0].getField( 0 );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Adding certificate ...", 0 );
    
    this.clientFactory.getBackendService( ).addCertificate( this.clientFactory.getLocalSession( ).getSession( ), userId, pem, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to add certificate", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to add certificate for user " + userId + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Certificate added", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "New certificate is added to user " + userId );
        //reloadCurrentRange( );
      }
      
    } );
  }

  @Override
  public void confirm( String subject ) {
    if ( DELETE_USERS_SUBJECT.equals( subject ) ) {
      doDeleteUsers( );
    } else if ( ADD_KEY_SUBJECT.equals( subject ) ) {
      doAddKey( );
    } else if ( APPROVE_USERS_SUBJECT.equals( subject ) ) {
      doApproveUsers( );
    } else if ( REJECT_USERS_SUBJECT.equals( subject ) ) {
      doRejectUsers( );
    }
  }

  private void doAddKey( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      return;
    }
    final String userId = this.currentSelected.toArray( new SearchResultRow[0] )[0].getField( 0 );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Adding access key ...", 0 );
    
    this.clientFactory.getBackendService( ).addAccessKey( this.clientFactory.getLocalSession( ).getSession( ), userId, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to add access key", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to add access key for user " + userId + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Access key added", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "New access key is added to user " + userId );
        //reloadCurrentRange( );
      }
      
    } );
  }

  private void doDeleteUsers( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      return;
    }
    
    final ArrayList<String> ids = Lists.newArrayList( ); 
    for ( SearchResultRow row : currentSelected ) {
      ids.add( row.getField( 0 ) );
    }
    
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Deleting users ...", 0 );
    
    clientFactory.getBackendService( ).deleteUsers( clientFactory.getLocalSession( ).getSession( ), ids, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to delete users", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to delete users " + ids + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Users deleted", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Users " + ids + " deleted" );
        reloadCurrentRange( );
      }
      
    } );
  }

  @Override
  public void onAction( String key ) {
    if ( ACTION_PASSWORD.equals( key ) ) {
      onChangePassword( );
    }
  }
  
  private void onChangePassword( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select a single user to change password", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( CHANGE_PASSWORD_CAPTION, CHANGE_PASSWORD_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

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
  
  private void doChangePassword( String oldPass, String newPass ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      return;
    }
    final String userId = this.currentSelected.toArray( new SearchResultRow[0] )[0].getField( 0 );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Changing password ...", 0 );
    
    this.clientFactory.getBackendService( ).changePassword( this.clientFactory.getLocalSession( ).getSession( ), userId, oldPass, newPass, null, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to change password", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to change password for user " + userId + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Password changed", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Password changed for user " + userId );
        //reloadCurrentRange( );
      }
      
    } );
  }

  @Override
  public void onApprove( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Select users to approve", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    ConfirmationView dialog = this.clientFactory.getConfirmationView( );
    dialog.setPresenter( this );
    dialog.display( APPROVE_USERS_CAPTION, APPROVE_USERS_SUBJECT, currentSelected, new ArrayList<Integer>( Arrays.asList( 0, 1 ) ) );
  }
  
  private void doApproveUsers( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      return;
    }
    final ArrayList<String> ids = Lists.newArrayList( ); 
    for ( SearchResultRow row : currentSelected ) {
      ids.add( row.getField( 0 ) );
    }
    
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Approving users ...", 0 );
    
    clientFactory.getBackendService( ).approveUsers( clientFactory.getLocalSession( ).getSession( ), ids, new AsyncCallback<ArrayList<String>>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to approve users", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to approve users " + ids + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( ArrayList<String> approved ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Users approved", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Users " + approved + " approved" );
        reloadCurrentRange( );
      }
      
    } );
  }
  
  @Override
  public void onReject( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Select users to reject", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    ConfirmationView dialog = this.clientFactory.getConfirmationView( );
    dialog.setPresenter( this );
    dialog.display( REJECT_USERS_CAPTION, REJECT_USERS_SUBJECT, currentSelected, new ArrayList<Integer>( Arrays.asList( 0, 1 ) ) );
  }
  
  private void doRejectUsers( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      return;
    }
    final ArrayList<String> ids = Lists.newArrayList( ); 
    for ( SearchResultRow row : currentSelected ) {
      ids.add( row.getField( 0 ) );
    }
    
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Rejecting users ...", 0 );
    
    clientFactory.getBackendService( ).rejectAccounts( clientFactory.getLocalSession( ).getSession( ), ids, new AsyncCallback<ArrayList<String>>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to reject users", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to reject users " + ids + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( ArrayList<String> approved ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Users rejected", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Users " + approved + " rejected" );
        reloadCurrentRange( );
      }
      
    } );
  }
  
}
