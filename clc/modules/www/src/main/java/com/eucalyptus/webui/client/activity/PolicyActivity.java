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
import com.eucalyptus.webui.client.view.ConfirmationView;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.PolicyView;
import com.eucalyptus.webui.client.view.HasValueWidget;
import com.eucalyptus.webui.client.view.FooterView.StatusType;
import com.eucalyptus.webui.client.view.LogView.LogType;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class PolicyActivity extends AbstractSearchActivity implements PolicyView.Presenter, ConfirmationView.Presenter {
  
  public static final String TITLE = "ACCESS POLICIES";
  
  public static final String DELETE_POLICY_CAPTION = "Delete selected policy";
  public static final String DELETE_POLICY_SUBJECT = "Are you sure you want to delete the following selected policy?";
  
  private static final Logger LOG = Logger.getLogger( PolicyActivity.class.getName( ) );
  
  private Set<SearchResultRow> currentSelected;
  
  public PolicyActivity( SearchPlace place, ClientFactory clientFactory ) {
    super( place, clientFactory );
  }

  @Override
  protected void doSearch( String query, SearchRange range ) {
    this.clientFactory.getBackendService( ).lookupPolicy( this.clientFactory.getLocalSession( ).getSession( ), search, range,
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
    // Nothing will happen here.
  }

  @Override
  protected String getTitle( ) {
    return TITLE;
  }

  @Override
  protected void showView( SearchResult result ) {
    if ( this.view == null ) {
      this.view = this.clientFactory.getPolicyView( );
      ( ( PolicyView ) this.view ).setPresenter( this );
      container.setWidget( this.view );
      ( ( PolicyView ) this.view ).clear( );
    }
    ( ( PolicyView ) this.view ).showSearchResult( result );    
  }

  @Override
  public void onDeletePolicy( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Select one policy to delete", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    ConfirmationView dialog = this.clientFactory.getConfirmationView( );
    dialog.setPresenter( this );
    dialog.display( DELETE_POLICY_CAPTION, DELETE_POLICY_SUBJECT, currentSelected, new ArrayList<Integer>( Arrays.asList( 0, 1 ) ) );
  }

  @Override
  public void confirm( String subject ) {
    if ( DELETE_POLICY_SUBJECT.equals( subject ) ) {
      doDeletePolicy( );
    }
  }

  private void doDeletePolicy( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      return;
    }
    
    final SearchResultRow policy = currentSelected.toArray( new SearchResultRow[0] )[0];
    final String policyId = emptyForNull( getField( policy.getRow( ), 0 ) );
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Deleting policy ...", 0 );
    
    clientFactory.getBackendService( ).deletePolicy( clientFactory.getLocalSession( ).getSession( ), policy, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to delete policy", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to delete policy " + policyId + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Policy deleted", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Policy " + policyId + " deleted" );
        reloadCurrentRange( );
      }
      
    } );
  }
  
}
