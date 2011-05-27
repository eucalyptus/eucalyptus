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
import com.eucalyptus.webui.client.view.CreateAccountView;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.FooterView.StatusType;
import com.eucalyptus.webui.client.view.HasValueWidget;
import com.eucalyptus.webui.client.view.LogView.LogType;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AccountActivity extends AbstractSearchActivity
    implements AccountView.Presenter, DetailView.Presenter, CreateAccountView.Presenter, ConfirmationView.Presenter {
  
  public static final String TITLE = "ACCOUNTS";
  public static final String CREATE_ACCOUNT_CAPTION = "Create a new account";
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
  public void saveValue( ArrayList<HasValueWidget> values ) {
    final ArrayList<String> newVals = Lists.newArrayList( );
    for ( HasValueWidget w : values ) {
      newVals.add( w.getValue( ) );
    }
    
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Modifying account " + newVals + " ...", 0 );
    
    clientFactory.getBackendService( ).modifyAccounts( clientFactory.getLocalSession( ).getSession( ), newVals, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to modify account", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to modify account with new values: " + newVals + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Successfully modified account", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Modified account with " + newVals );
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
    CreateAccountView dialog = this.clientFactory.getCreateAccountView( );
    dialog.setPresenter( this );
    dialog.display( CREATE_ACCOUNT_CAPTION );
  }

  @Override
  public void doCreateAccount( final String value ) {
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
  public void onDeleteAccounts( ) {
    if ( currentSelected != null ) {
      ConfirmationView dialog = this.clientFactory.getConfirmationView( );
      dialog.setPresenter( this );
      dialog.display( DELETE_ACCOUNTS_CAPTION, DELETE_ACCOUNTS_SUBJECT, currentSelected, new ArrayList<Integer>( Arrays.asList( 0, 1 ) ) );
    }
  }

  @Override
  public void confirm( String subject ) {
    if ( DELETE_ACCOUNTS_SUBJECT.equals( subject ) ) {
      deleteSelectedAccounts( );
    }
  }
  
  private void deleteSelectedAccounts( ) {
    if ( currentSelected != null ) {
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
  }
}
