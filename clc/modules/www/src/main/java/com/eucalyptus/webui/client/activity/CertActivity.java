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
import com.eucalyptus.webui.client.view.CertView;
import com.eucalyptus.webui.client.view.ConfirmationView;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.HasValueWidget;
import com.eucalyptus.webui.client.view.FooterView.StatusType;
import com.eucalyptus.webui.client.view.LogView.LogType;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class CertActivity extends AbstractSearchActivity implements CertView.Presenter, ConfirmationView.Presenter {
  
  public static final String TITLE = "X509 CERTIFICATES";
  
  public static final String DELETE_CERT_CAPTION = "Delete selected certificate";
  public static final String DELETE_CERT_SUBJECT = "Are you sure you want to delete the following selected certificate?";
  
  private static final Logger LOG = Logger.getLogger( CertActivity.class.getName( ) );
  
  private Set<SearchResultRow> currentSelected;
  
  public CertActivity( SearchPlace place, ClientFactory clientFactory ) {
    super( place, clientFactory );
  }

  @Override
  protected void doSearch( String query, SearchRange range ) {
    this.clientFactory.getBackendService( ).lookupCertificate( this.clientFactory.getLocalSession( ).getSession( ), search, range,
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
    
    final String certId = emptyForNull( getField( newVals, 0 ) );
    
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Modifying certificate " + certId + " ...", 0 );
    
    clientFactory.getBackendService( ).modifyCertificate( clientFactory.getLocalSession( ).getSession( ), newVals, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to modify certificate", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to modify certificate " + certId  + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Successfully modified certificate", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Modified certificate " + certId );
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
      this.view = this.clientFactory.getCertView( );
      ( ( CertView ) this.view ).setPresenter( this );
      container.setWidget( this.view );
      ( ( CertView ) this.view ).clear( );
    }
    ( ( CertView ) this.view ).showSearchResult( result );    
  }

  @Override
  public void confirm( String subject ) {
    if ( DELETE_CERT_SUBJECT.equals( subject ) ) {
      doDeleteCert( );
    }
  }

  private void doDeleteCert( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      return;
    }
    
    final SearchResultRow cert = currentSelected.toArray( new SearchResultRow[0] )[0];
    final String certId = emptyForNull( getField( cert.getRow( ), 0 ) );
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Deleting certificate ...", 0 );
    
    clientFactory.getBackendService( ).deleteCertificate( clientFactory.getLocalSession( ).getSession( ), cert, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to delete certificate", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to delete certificate " + certId + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Certificate deleted", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Certificate " + certId + " deleted" );
        reloadCurrentRange( );
      }
      
    } );
  }


  @Override
  public void onDeleteCert( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Select one certificate to delete", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    ConfirmationView dialog = this.clientFactory.getConfirmationView( );
    dialog.setPresenter( this );
    dialog.display( DELETE_CERT_CAPTION, DELETE_CERT_SUBJECT, currentSelected, new ArrayList<Integer>( Arrays.asList( 0, 1 ) ) );
  }
  
}
