package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.SearchPlace;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.service.SearchResultCache;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.session.SessionData;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.ErrorSinkView;
import com.eucalyptus.webui.client.view.KnowsPageSize;
import com.eucalyptus.webui.client.view.LoadingAnimationView;
import com.eucalyptus.webui.client.view.SearchRangeChangeHandler;
import com.eucalyptus.webui.client.view.SelectionController;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * Boilerplate of a search activity.
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 *
 */
public abstract class AbstractSearchActivity extends AbstractActivity implements DetailView.Presenter, SearchRangeChangeHandler, KnowsPageSize {
  
  private static final Logger LOG = Logger.getLogger( AbstractSearchActivity.class.getName( ) );
  
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
    
  public AbstractSearchActivity( SearchPlace place, ClientFactory clientFactory ) {
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
    
    LOG.log( Level.INFO, "Search " + getTitle( ) + ": " + place.getSearch( ) );
    // At the beginning, don't sort
    range = new SearchRange( 0, pageSize, -1/*sortField*/, true );
    doSearch( place.getSearch( ), range );
    
    ActivityUtil.updateDirectorySelection( clientFactory );
  }
  
  @Override
  public void onStop( ) {
    this.clientFactory.getShellView( ).getDetailView( ).clear( );
    this.clientFactory.getShellView( ).hideDetail( );
  }

  @Override
  public void onCancel( ) {
    this.clientFactory.getShellView( ).getDetailView( ).clear( );
    this.clientFactory.getShellView( ).hideDetail( );
  }

  protected void displayData( SearchResult result ) {
    if ( this.place != this.clientFactory.getMainPlaceController( ).getWhere( ) ) {
      LOG.log( Level.INFO, "Place was changed prematurely" );
      return;
    }
    if ( result != null ) {
      LOG.log( Level.INFO, "Received " + result );
      cache.update( result );
      showView( result );
    } else {
      ErrorSinkView errorView = this.clientFactory.getErrorSinkView( );
      errorView.setMessage( "Search '" + search + "' failed." );
      container.setWidget( errorView );
    }
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
  
  protected void showSingleSelectedDetails( SearchResultRow selected ) {
    ArrayList<SearchResultFieldDesc> descs = new ArrayList<SearchResultFieldDesc>( );
    descs.addAll( cache.getDescs( ) );
    descs.addAll( selected.getExtraFieldDescs( ) );
    this.clientFactory.getShellView( ).getDetailView( ).showData( descs, selected.getRow( ) );          
  }
 
  @Override
  public void onAction( String key ) {
    //Nothing to do.
  }
  
  @Override
  public void onHide( ) {
    if ( this.view != null && this.view instanceof SelectionController ) {
      ( ( SelectionController )this.view ).clearSelection( );
    }
  }
  
  public void cancel( String subject ) {
    //Nothing to do.
  }
  
  protected static String getField( ArrayList<String> values, int index ) {
    if ( values != null && values.size( ) > index ) {
      return values.get( index );
    }
    return null;
  }
  
  protected static String emptyForNull( String s ) {
    return s == null ? "" : s;
  }
  
  protected abstract void doSearch( String query, SearchRange range );
  
  protected abstract String getTitle( );
  
  protected abstract void showView( SearchResult result );
  
}
