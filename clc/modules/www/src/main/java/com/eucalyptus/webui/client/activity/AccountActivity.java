package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.AccountPlace;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.view.AccountView;
import com.eucalyptus.webui.client.view.ConfirmationView;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.InputField;
import com.eucalyptus.webui.client.view.InputView;
import com.eucalyptus.webui.client.view.FooterView.StatusType;
import com.eucalyptus.webui.client.view.HasValueWidget;
import com.eucalyptus.webui.client.view.LogView.LogType;
import com.eucalyptus.webui.shared.checker.ValueChecker;
import com.eucalyptus.webui.shared.checker.ValueCheckerFactory;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AccountActivity extends AbstractSearchActivity
    implements AccountView.Presenter, DetailView.Presenter, ConfirmationView.Presenter, InputView.Presenter {
  
  public static final String TITLE = "ACCOUNTS";
  
  public static final String CREATE_ACCOUNT_CAPTION = "Create a new account";
  public static final String CREATE_ACCOUNT_SUBJECT = "Enter information to create a new account:";
  public static final String ACCOUNT_NAME_INPUT_TITLE = "Account name";
  
  public static final String CREATE_USERS_CAPTION = "Create new users";
  public static final String CREATE_USERS_SUBJECT = "Enter information to create new users (using space to separate names):";
  public static final String USER_NAMES_INPUT_TITLE = "User names";
  public static final String USER_PATH_INPUT_TITLE = "User path";
  
  public static final String CREATE_GROUPS_CAPTION = "Create new groups";
  public static final String CREATE_GROUPS_SUBJECT = "Enter information to create new groups (using space to separate names):";
  public static final String GROUP_NAMES_INPUT_TITLE = "Group names";
  public static final String GROUP_PATH_INPUT_TITLE = "Group path";
  
  public static final String ADD_POLICY_CAPTION = "Add new policy";
  public static final String ADD_POLICY_SUBJECT = "Enter new policy to assign to the selected account:";
  public static final String POLICY_NAME_INPUT_TITLE = "Policy name";
  public static final String POLICY_CONTENT_INPUT_TITLE = "Policy content";
  
  public static final String DELETE_ACCOUNTS_CAPTION = "Delete selected accounts";
  public static final String DELETE_ACCOUNTS_SUBJECT = "Are you sure to delete following selected accounts?";
  
  private static final Logger LOG = Logger.getLogger( AccountActivity.class.getName( ) );
  
  private Set<SearchResultRow> currentSelected;
    
  public AccountActivity( AccountPlace place, ClientFactory clientFactory ) {
    super( place, clientFactory );
  }

  protected void doSearch( final String search, SearchRange range ) {    
    this.clientFactory.getBackendService( ).lookupAccount( this.clientFactory.getLocalSession( ).getSession( ), search, range,
                                                           new AsyncCallback<SearchResult>( ) {
      
      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Search failed: " + caught );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Account search " + search + " failed: " + caught.getMessage( ) );
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
    
    final String accountId = emptyForNull( getField( newVals, 0 ) );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Modifying account " + accountId + " ...", 0 );
    
    clientFactory.getBackendService( ).modifyAccount( clientFactory.getLocalSession( ).getSession( ), newVals, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to modify account", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to modify account " + accountId  + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Successfully modified account", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Modified account " + accountId );
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
      this.view = this.clientFactory.getAccountView( );
      ( ( AccountView ) this.view ).setPresenter( this );
      container.setWidget( this.view );
      ( ( AccountView ) this.view ).clear( );
    }
    ( ( AccountView ) this.view ).showSearchResult( result );    
  }

  @Override
  public void onCreateAccount( ) {
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( CREATE_ACCOUNT_CAPTION, CREATE_ACCOUNT_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return ACCOUNT_NAME_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createAccountNameChecker( );
      }
      
    } ) ) );
  }

  @Override
  public void onDeleteAccounts( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Select accounts to delete", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    ConfirmationView dialog = this.clientFactory.getConfirmationView( );
    dialog.setPresenter( this );
    dialog.display( DELETE_ACCOUNTS_CAPTION, DELETE_ACCOUNTS_SUBJECT, currentSelected, new ArrayList<Integer>( Arrays.asList( 0, 1 ) ) );
  }

  @Override
  public void confirm( String subject ) {
    if ( DELETE_ACCOUNTS_SUBJECT.equals( subject ) ) {
      doDeleteAccounts( );
    }
  }
  
  private void doDeleteAccounts( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      return;
    }
    
    final ArrayList<String> ids = Lists.newArrayList( ); 
    for ( SearchResultRow row : currentSelected ) {
      ids.add( row.getField( 0 ) );
    }
    
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Deleting accounts ...", 0 );
    
    clientFactory.getBackendService( ).deleteAccounts( clientFactory.getLocalSession( ).getSession( ), ids, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to delete accounts", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to delete accounts " + ids + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Accounts deleted", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Accounts " + ids + " deleted" );
        reloadCurrentRange( );
      }
      
    } );
  }

  @Override
  public void process( String subject, ArrayList<String> values ) {
    if ( CREATE_ACCOUNT_SUBJECT.equals( subject ) ) {
      doCreateAccount( values.get( 0 ) );
    } else if ( CREATE_USERS_SUBJECT.equals( subject ) ) {
      doCreateUsers( values.get( 0 ), values.get( 1 ) );
    } else if ( CREATE_GROUPS_SUBJECT.equals( subject ) ) {
      doCreateGroups( values.get( 0 ), values.get( 1 ) );
    } else if ( ADD_POLICY_SUBJECT.equals( subject ) ) {
      doAddPolicy( values.get( 0 ), values.get( 1 ) );
    }
  }

  private void doCreateAccount( final String value ) {
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Creating account " + value + " ...", 0 );
    
    this.clientFactory.getBackendService( ).createAccount( this.clientFactory.getLocalSession( ).getSession( ), value, new AsyncCallback<String>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to create account", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Creating account " + value + " failed: " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( String accountId ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Account " + accountId + " created", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "New account " + accountId + " created" );
        reloadCurrentRange( );
      }
      
    } );
  }

  @Override
  public void onCreateUsers( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select a single account to create users", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( CREATE_USERS_CAPTION, CREATE_USERS_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return USER_NAMES_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createUserAndGroupNamesChecker( );
      }
      
    }, new InputField( ) {

      @Override
      public String getTitle( ) {
        return USER_PATH_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createPathChecker( );
      }
      
    } ) ) );    
  }

  private void doCreateUsers( final String names, final String path ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      return;
    }
    final String accountId = this.currentSelected.toArray( new SearchResultRow[0] )[0].getField( 0 );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Creating user " + names + " ...", 0 );
    
    this.clientFactory.getBackendService( ).createUsers( this.clientFactory.getLocalSession( ).getSession( ), accountId, names, path, new AsyncCallback<ArrayList<String>>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to create users", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Creating users " + names + "for account " + accountId + " failed: " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( ArrayList<String> created ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, created.size( ) + " users created", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "New users " + created + " created for account " + accountId );
        //reloadCurrentRange( );
      }
      
    } );
  }
  
  @Override
  public void onCreateGroups( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select a single account to create groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( CREATE_GROUPS_CAPTION, CREATE_GROUPS_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

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
      
    }, new InputField( ) {

      @Override
      public String getTitle( ) {
        return GROUP_PATH_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createPathChecker( );
      }
      
    } ) ) );
  }

  private void doCreateGroups( final String names, final String path ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      return;
    }
    final String accountId = this.currentSelected.toArray( new SearchResultRow[0] )[0].getField( 0 );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Creating group " + names + " ...", 0 );
    
    this.clientFactory.getBackendService( ).createGroups( this.clientFactory.getLocalSession( ).getSession( ), accountId, names, path, new AsyncCallback<ArrayList<String>>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to create groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Creating groups " + names + " for account " + accountId + " failed: " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( ArrayList<String> created ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, created.size( ) + " groups created", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "New groups " + created + " created for account " + accountId );
        //reloadCurrentRange( );
      }
      
    } );
  }
  
  @Override
  public void onAddPolicy( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select a single account to add policy", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
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

  private void doAddPolicy( final String name, String document ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      return;
    }
    final String accountId = this.currentSelected.toArray( new SearchResultRow[0] )[0].getField( 0 );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Adding policy " + name + " ...", 0 );
    
    this.clientFactory.getBackendService( ).addAccountPolicy( this.clientFactory.getLocalSession( ).getSession( ), accountId, name, document, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to add policy", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to add policy " + name + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Policy added", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "New policy " + name + " is added to account " + accountId );
        //reloadCurrentRange( );
      }
      
    } );
  }
  
}
