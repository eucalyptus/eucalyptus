package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.ConfigPlace;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.HasValueWidget;
import com.eucalyptus.webui.client.view.ConfigView;
import com.eucalyptus.webui.client.view.FooterView.StatusType;
import com.eucalyptus.webui.client.view.LogView.LogType;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ConfigActivity extends AbstractSearchActivity implements ConfigView.Presenter, DetailView.Presenter {
  
  public static final String TITLE = "SERVICE COMPONENTS";
  
  private static final Logger LOG = Logger.getLogger( ConfigActivity.class.getName( ) );

  protected SearchResultRow currentSelected = null;

  public ConfigActivity( ConfigPlace place, ClientFactory clientFactory ) {
    super( place, clientFactory );
  }
  
  @Override
  protected void showView( SearchResult result ) {
    if ( this.view == null ) {
      this.view = this.clientFactory.getConfigView( );
      ( ( ConfigView ) this.view ).setPresenter( this );
      container.setWidget( this.view );
      ( ( ConfigView ) this.view ).clear( );
    }
    ( ( ConfigView ) this.view ).showSearchResult( result );
  }

  @Override
  protected void doSearch( String query, SearchRange range ) {
    LOG.log( Level.INFO, "'service' new search: " + query );
    this.clientFactory.getBackendService( ).lookupConfiguration( this.clientFactory.getLocalSession( ).getSession( ), query, range, new AsyncCallback<SearchResult>( ) {

      @Override
      public void onFailure( Throwable cause ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, cause );
        LOG.log( Level.WARNING, "Failed to get configurations: " + cause );
        displayData( null );
      }

      @Override
      public void onSuccess( SearchResult result ) {
        displayData( result );
      }
      
    } );
  }

  @Override
  public void onSelectionChange( SearchResultRow selection ) {
    this.currentSelected = selection;
    if ( selection == null ) {
      LOG.log( Level.INFO, "Selection changed to null" );      
      this.clientFactory.getShellView( ).hideDetail( );
    } else {
      LOG.log( Level.INFO, "Selection changed to " + selection );
      this.clientFactory.getShellView( ).showDetail( DETAIL_PANE_SIZE );
      showSingleSelectedDetails( selection );
    }
  }

  @Override
  public void saveValue( ArrayList<String> keys, ArrayList<HasValueWidget> values ) {
    if ( values == null || values.size( ) < 1 || this.currentSelected == null ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select a single service component to change value", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      LOG.log( Level.WARNING, "No valid values or empty selection" );
      return;
    }
    final SearchResultRow result = new SearchResultRow( );
    result.setExtraFieldDescs( this.currentSelected.getExtraFieldDescs( ) );
    for ( int i = 0; i < values.size( ); i++ ) {
      result.addField( values.get( i ).getValue( ) );
    }
    
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Changing service configuration ...", 0 );
    
    this.clientFactory.getBackendService( ).setConfiguration( this.clientFactory.getLocalSession( ).getSession( ), result, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable cause ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, cause );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to change service configuration", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to change service configuration: " + result );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Service configuration changed", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Successfully changed service configuration: " + result );        
        clientFactory.getShellView( ).getDetailView( ).disableSave( );
        reloadCurrentRange( );
      }
      
    } );
  }

  @Override
  protected String getTitle( ) {
    return TITLE;
  }

}
