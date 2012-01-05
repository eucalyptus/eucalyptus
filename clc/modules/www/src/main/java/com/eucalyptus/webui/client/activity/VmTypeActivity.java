package com.eucalyptus.webui.client.activity;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.VmTypePlace;
import com.eucalyptus.webui.client.service.SearchRange;
import com.eucalyptus.webui.client.service.SearchResult;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.HasValueWidget;
import com.eucalyptus.webui.client.view.VmTypeView;
import com.eucalyptus.webui.client.view.FooterView.StatusType;
import com.eucalyptus.webui.client.view.LogView.LogType;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class VmTypeActivity extends AbstractSearchActivity implements VmTypeView.Presenter, DetailView.Presenter {

  public static final String TITLE = "VIRTUAL MACHINE TYPES";
  
  private static final Logger LOG = Logger.getLogger( ConfigActivity.class.getName( ) );
    
  protected SearchResultRow currentSelected = null;

  public VmTypeActivity( VmTypePlace place, ClientFactory clientFactory ) {
    super( place, clientFactory );
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
      showSelectedDetails( );
    }
  }

  private void showSelectedDetails( ) {
    ArrayList<SearchResultFieldDesc> descs = new ArrayList<SearchResultFieldDesc>( );
    descs.addAll( cache.getDescs( ) );
    descs.addAll( currentSelected.getExtraFieldDescs( ) );
    this.clientFactory.getShellView( ).getDetailView( ).showData( descs, currentSelected.getRow( ) );          
  }

  @Override
  public void saveValue( ArrayList<String> keys, ArrayList<HasValueWidget> values ) {
    if ( values == null || values.size( ) < 1 || this.currentSelected == null ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select a single vm type to change value", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      LOG.log( Level.WARNING, "No valid values or empty selection" );
      return;
    }
    LOG.log( Level.INFO, "Saving: " + values );
    final SearchResultRow result = new SearchResultRow( );
    result.setExtraFieldDescs( this.currentSelected.getExtraFieldDescs( ) );
    for ( int i = 0; i < values.size( ); i++ ) {
      result.addField( values.get( i ).getValue( ) );
    }
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Changing VM type definition ...", 0 );
    
    this.clientFactory.getBackendService( ).setVmType( this.clientFactory.getLocalSession( ).getSession( ), result, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable cause ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, cause );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to change VM type: " + cause.getMessage( ), FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to change VM type: " + cause.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "VM type changed", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Successfully changed VM type: " + result );        
        clientFactory.getShellView( ).getDetailView( ).disableSave( );
        reloadCurrentRange( );
      }
      
    } );
  }

  @Override
  protected void doSearch( String query, SearchRange range ) {
    LOG.log( Level.INFO, "'service' new search: " + query );
    this.clientFactory.getBackendService( ).lookupVmType( this.clientFactory.getLocalSession( ).getSession( ), query, range, new AsyncCallback<SearchResult>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        displayData( null );
      }

      @Override
      public void onSuccess( SearchResult result ) {
        displayData( result );
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
      this.view = this.clientFactory.getVmTypeView( );
      ( ( VmTypeView ) this.view ).setPresenter( this );
      container.setWidget( this.view );
      ( ( VmTypeView ) this.view ).clear( );
    }
    ( ( VmTypeView ) this.view ).showSearchResult( result );
  }

}
