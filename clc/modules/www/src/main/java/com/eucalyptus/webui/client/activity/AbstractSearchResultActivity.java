package com.eucalyptus.webui.client.activity;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.SearchPlace;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.service.SearchResultCache;
import com.eucalyptus.webui.client.session.SessionData;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.KnowsPageSize;
import com.eucalyptus.webui.client.view.LoadingAnimationView;
import com.eucalyptus.webui.client.view.SearchRangeChangeHandler;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

public abstract class AbstractSearchResultActivity extends AbstractActivity implements DetailView.Presenter, SearchRangeChangeHandler, KnowsPageSize {
  
  private static final Logger LOG = Logger.getLogger( AbstractSearchResultActivity.class.getName( ) );
  
  protected static final int DEFAULT_PAGE_SIZE = 25;
  protected static final int DETAIL_PANE_SIZE = 400;//px
  
  protected ClientFactory clientFactory;
  protected int pageSize;
  
  protected SearchPlace place;
  protected String search;
  protected SearchRange range;
  protected SearchResultCache cache = new SearchResultCache( );
  
  protected AcceptsOneWidget container;
  protected IsWidget view;
    
  public AbstractSearchResultActivity( SearchPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.search = URL.decode( place.getSearch( ) );
    this.clientFactory = clientFactory;
    this.pageSize = this.clientFactory.getSessionData( ).getIntProperty( SessionData.SEARCH_RESULT_PAGE_SIZE, DEFAULT_PAGE_SIZE ); 
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    this.container = container;
    // Hide detail view at the beginning
    this.clientFactory.getShellView( ).hideDetail( );
    this.clientFactory.getShellView( ).getDetailView( ).setPresenter( this );
    
    this.clientFactory.getShellView( ).getContentView( ).setContentTitle( getTitle( ) );
    
    // Show loading first
    LoadingAnimationView view = this.clientFactory.getLoadingAnimationView( );
    container.setWidget( view );
    
    doSearch( URL.decode( place.getSearch( ) ), new SearchRange( 0, pageSize ) );
  }
  
  @Override
  public void onStop( ) {
    this.clientFactory.getShellView( ).getDetailView( ).clear( );
    this.clientFactory.getShellView( ).hideDetail( );
  }
  
  protected void displayData( SearchResult result ) {
    LOG.log( Level.INFO, "Received " + result );
    cache.update( result );
    showView( result );
  }
  
  @Override
  public void handleRangeChange( SearchRange range ) {
    this.range = range;
    SearchResult result = cache.lookup( range );
    if ( result != null ) {
      // Use the cached result if search range does not change
      showView( result );
    } else {
      doSearch( this.search, range );
    }
  }
  
  @Override
  public int getPageSize( ) {
    return pageSize;
  }
  
  protected void reloadCurrentRange( ) {
    if ( this.range != null ) {
      cache.clear( );
      doSearch( this.search, range );      
    }
  }
  
  protected abstract void doSearch( String query, SearchRange range );
  
  protected abstract String getTitle( );
  
  protected abstract void showView( SearchResult result );
  
}
