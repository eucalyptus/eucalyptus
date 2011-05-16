package com.eucalyptus.webui.client.activity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.AccountPlace;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResultCache;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.session.SessionData;
import com.eucalyptus.webui.client.view.AccountView;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.LoadingAnimationView;
import com.eucalyptus.webui.client.view.SearchRangeChangeHandler;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class AccountActivity extends AbstractActivity implements AccountView.Presenter {
  
  public static final String TITLE = "USER ACCOUNTS";
  
  private static final Logger LOG = Logger.getLogger( AccountActivity.class.getName( ) );
  
  private static final int DEFAULT_PAGE_SIZE = 25;
  
  private static final int DETAIL_PANE_SIZE = 360;//px
  
  private AccountPlace place;
  private ClientFactory clientFactory;
  
  private String search;
  private SearchResultCache cache = new SearchResultCache( );

  private AcceptsOneWidget container;
  private AccountView view = null;
  private int pageSize;
  
  
  public AccountActivity( AccountPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.search = URL.decode( place.getSearch( ) );
    this.clientFactory = clientFactory;
    this.pageSize = this.clientFactory.getSessionData( ).getIntProperty( SessionData.SEARCH_RESULT_PAGE_SIZE, DEFAULT_PAGE_SIZE );
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    this.container = container;
    // Set title
    this.clientFactory.getShellView( ).getContentView( ).setContentTitle( TITLE );
    // Show loading first
    LoadingAnimationView view = this.clientFactory.getLoadingAnimationView( );
    container.setWidget( view );
    // Start a search
    doSearch( search, new SearchRange( 0, pageSize ) );
  }

  private void showView( SearchResult result ) {    
    this.view = this.clientFactory.getAccountView( );
    this.view.setPresenter( this );
    container.setWidget( this.view );
  }
  
  private void displayData( SearchResult result ) {
    cache.update( result );
    
    for ( SearchResultRow row : result.getRows( ) ) {
      LOG.log( Level.INFO, "Row: " + row );
    }
    if ( this.view == null ) {
      showView( result );
    }
    this.view.showSearchResult( result );
  }
  
  private void doSearch( String search, SearchRange range ) {    
    this.clientFactory.getBackendService( ).lookupAccount( this.clientFactory.getLocalSession( ).getSession( ), search, range,
                                                           new AsyncCallback<SearchResult>( ) {
      
      @Override
      public void onFailure( Throwable caught ) {
        LOG.log( Level.WARNING, "Search failed: " + caught );
        displayData( null );
      }
      
      @Override
      public void onSuccess( SearchResult result ) {
        LOG.log( Level.INFO, "Search success:" + result.length( ) );
        displayData( result );
      }
      
    } );
  }
  
  @Override
  public void handleRangeChange( SearchRange range ) {
    LOG.log( Level.INFO, "Account search range change: " + range );
    SearchResult result = cache.lookup( range );
    if ( result != null ) {
      // Use the cached result if search range does not change
      displayData( result );
    } else {
      doSearch( this.search, range );
    }
  }

  @Override
  public void onSelectionChange( Set<SearchResultRow> selection ) {
    LOG.log( Level.INFO, "Showing detail." );
    this.clientFactory.getShellView( ).showDetail( DETAIL_PANE_SIZE );
  }

  @Override
  public int getPageSize( ) {
    return this.pageSize;
  }
  
}
