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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.RootPanel;

public class AccountActivity extends AbstractSearchActivity
    implements AccountView.Presenter, DetailView.Presenter, CreateAccountView.Presenter {
  
  public static final String TITLE = "ACCOUNTS";
  
  private static final Logger LOG = Logger.getLogger( AccountActivity.class.getName( ) );
  
  private Set<SearchResultRow> currentSelected;
  
  private DialogBox createAccountDialog;
  
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
  public void createAccount( ) {
    if ( createAccountDialog == null ) {
      createAccountDialog = new DialogBox( );
      createAccountDialog.setText( "Create a new account" );
      createAccountDialog.setWidget( this.clientFactory.getCreateAccountView( ) );
      createAccountDialog.setGlassEnabled( true );
    }
    this.clientFactory.getCreateAccountView( ).init( );
    this.clientFactory.getCreateAccountView( ).setPresenter( this );
    createAccountDialog.center( );
    createAccountDialog.show( );
  }

  @Override
  public void createAccount( String value ) {
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Creating account " + value + " ...", 2000 );
    this.createAccountDialog.hide( );
  }

  @Override
  public void cancel( ) {
    this.createAccountDialog.hide( );
  }
  
}
