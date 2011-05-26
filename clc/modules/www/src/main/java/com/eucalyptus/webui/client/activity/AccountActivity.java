package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.AccountPlace;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.view.AccountView;
import com.eucalyptus.webui.client.view.CreateAccountView;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.FooterView.StatusType;
import com.eucalyptus.webui.client.view.HasValueWidget;
import com.eucalyptus.webui.client.view.LogView.LogType;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;

public class AccountActivity extends AbstractSearchActivity
    implements AccountView.Presenter, DetailView.Presenter, CreateAccountView.Presenter {
  
  public static final String TITLE = "ACCOUNTS";
  public static final String CREATE_ACCOUNT_CAPTION = "Create a new account";
  
  private static final Logger LOG = Logger.getLogger( AccountActivity.class.getName( ) );
  
  private Set<SearchResultRow> currentSelected;
    
  public AccountActivity( AccountPlace place, ClientFactory clientFactory ) {
    super( place, clientFactory );
  }

  protected void doSearch( String search, SearchRange range ) {    
    this.clientFactory.getBackendService( ).lookupAccount( this.clientFactory.getLocalSession( ).getSession( ), search, range,
                                                           new AsyncCallback<SearchResult>( ) {
      
      @Override
      public void onFailure( Throwable caught ) {
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
  public void saveValue( ArrayList<HasValueWidget> values ) {
    // TODO Auto-generated method stub
    
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
    
    this.clientFactory.getBackendService( ).createAccount( this.clientFactory.getLocalSession( ).getSession( ), value, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        String error = "Failed to create account " + value + ": " + caught.getMessage( );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, error, 60000 );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, error );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        String info = "Account " + value + " created";
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, info, 60000 );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, info );
        reloadCurrentRange( );
      }
      
    } );
  }
  
}
