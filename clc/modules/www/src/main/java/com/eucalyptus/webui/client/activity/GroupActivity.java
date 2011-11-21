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
import com.eucalyptus.webui.client.view.FooterView;
import com.eucalyptus.webui.client.view.GroupView;
import com.eucalyptus.webui.client.view.DetailView;
import com.eucalyptus.webui.client.view.HasValueWidget;
import com.eucalyptus.webui.client.view.InputField;
import com.eucalyptus.webui.client.view.FooterView.StatusType;
import com.eucalyptus.webui.client.view.LogView.LogType;
import com.eucalyptus.webui.client.view.InputView;
import com.eucalyptus.webui.shared.checker.ValueChecker;
import com.eucalyptus.webui.shared.checker.ValueCheckerFactory;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class GroupActivity extends AbstractSearchActivity
    implements GroupView.Presenter, ConfirmationView.Presenter, InputView.Presenter {
  
  public static final String TITLE = "GROUPS";
  
  public static final String DELETE_GROUPS_CAPTION = "Delete selected groups";
  public static final String DELETE_GROUPS_SUBJECT = "Are you sure you want to delete following selected groups?";
  
  public static final String ADD_USERS_CAPTION = "Add users to selected groups";
  public static final String ADD_USERS_SUBJECT = "Enter users to add to selected groups (using space to separate names):";
  public static final String USER_NAMES_INPUT_TITLE = "User names";

  public static final String REMOVE_USERS_CAPTION = "Remove users from selected groups";
  public static final String REMOVE_USERS_SUBJECT = "Enter users to remove from selected groups (using space to separate names):";

  public static final String ADD_POLICY_CAPTION = "Add new policy";
  public static final String ADD_POLICY_SUBJECT = "Enter new policy to assign to the selected group:";
  public static final String POLICY_NAME_INPUT_TITLE = "Policy name";
  public static final String POLICY_CONTENT_INPUT_TITLE = "Policy content";

  private static final Logger LOG = Logger.getLogger( GroupActivity.class.getName( ) );
  
  private Set<SearchResultRow> currentSelected;
  
  public GroupActivity( SearchPlace place, ClientFactory clientFactory ) {
    super( place, clientFactory );
  }

  @Override
  protected void doSearch( String query, SearchRange range ) {
    this.clientFactory.getBackendService( ).lookupGroup( this.clientFactory.getLocalSession( ).getSession( ), search, range,
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
    
    final String groupId = emptyForNull( getField( newVals, 0 ) );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Modifying group " + groupId + " ...", 0 );
    
    clientFactory.getBackendService( ).modifyGroup( clientFactory.getLocalSession( ).getSession( ), newVals, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to modify group", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to modify group " + groupId  + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Successfully modified group", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Modified group " + groupId );
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
      this.view = this.clientFactory.getGroupView( );
      ( ( GroupView ) this.view ).setPresenter( this );
      container.setWidget( this.view );
      ( ( GroupView ) this.view ).clear( );
    }
    ( ( GroupView ) this.view ).showSearchResult( result );    
  }

  @Override
  public void onDeleteGroup( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Select groups to delete", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    ConfirmationView dialog = this.clientFactory.getConfirmationView( );
    dialog.setPresenter( this );
    dialog.display( DELETE_GROUPS_CAPTION, DELETE_GROUPS_SUBJECT, currentSelected, new ArrayList<Integer>( Arrays.asList( 0, 1 ) ) );
  }

  @Override
  public void onAddUsers( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select groups to add users", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( ADD_USERS_CAPTION, ADD_USERS_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return USER_NAMES_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createUserAndGroupNamesChecker( );
      }
      
    } ) ) );    
  }

  @Override
  public void onRemoveUsers( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select groups for user removal", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( REMOVE_USERS_CAPTION, REMOVE_USERS_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return USER_NAMES_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createUserAndGroupNamesChecker( );
      }
      
    } ) ) );
  }

  @Override
  public void onAddPolicy( ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Must select a single group to add policy", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
      return;
    }
    InputView dialog = this.clientFactory.getInputView( );
    dialog.setPresenter( this );
    dialog.display( ADD_POLICY_CAPTION, ADD_POLICY_SUBJECT, new ArrayList<InputField>( Arrays.asList( new InputField( ) {

      @Override
      public String getTitle( ) {
        return POLICY_NAME_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXT;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createPolicyNameChecker( );
      }
      
    }, new InputField( ) {

      @Override
      public String getTitle( ) {
        return POLICY_CONTENT_INPUT_TITLE;
      }

      @Override
      public ValueType getType( ) {
        return ValueType.TEXTAREA;
      }

      @Override
      public ValueChecker getChecker( ) {
        return ValueCheckerFactory.createNonEmptyValueChecker( );
      }
      
    } ) ) );
  }

  @Override
  public void process( String subject, ArrayList<String> values ) {
    if ( ADD_USERS_SUBJECT.equals( subject ) ) {
      doAddUsers( values.get( 0 ) );
    } else if ( REMOVE_USERS_SUBJECT.equals( subject ) ) {
      doRemoveUsers( values.get( 0 ) );
    } else if ( ADD_POLICY_SUBJECT.equals( subject ) ) {
      doAddPolicy( values.get( 0 ), values.get( 1 ) );
    }
  }

  private void doAddPolicy( final String name, final String document ) {
    if ( currentSelected == null || currentSelected.size( ) != 1 ) {
      return;
    }
    final String groupId = this.currentSelected.toArray( new SearchResultRow[0] )[0].getField( 0 );
    this.clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Adding policy " + name + " ...", 0 );
    
    this.clientFactory.getBackendService( ).addGroupPolicy( this.clientFactory.getLocalSession( ).getSession( ), groupId, name, document, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to add policy", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to add policy " + name + " for group " + groupId + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Policy added", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "New policy " + name + " is added to group " + groupId );
        //reloadCurrentRange( );
      }
      
    } );
  }

  private void doRemoveUsers( final String names ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      return;
    }
    
    final ArrayList<String> ids = Lists.newArrayList( ); 
    for ( SearchResultRow row : currentSelected ) {
      ids.add( row.getField( 0 ) );
    }
    
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Removing users from selected groups ...", 0 );
    
    clientFactory.getBackendService( ).removeUsersFromGroupsByName( clientFactory.getLocalSession( ).getSession( ), names, ids, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to remove users", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to remove users " + names + " from groups " + ids + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Users removed from selected groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Users " + names + " are removed from groups " + ids );
      }
      
    } );
  }

  private void doAddUsers( final String names ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      return;
    }
    
    final ArrayList<String> ids = Lists.newArrayList( ); 
    for ( SearchResultRow row : currentSelected ) {
      ids.add( row.getField( 0 ) );
    }
    
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Adding users to selected groups ...", 0 );
    
    clientFactory.getBackendService( ).addUsersToGroupsByName( clientFactory.getLocalSession( ).getSession( ), names, ids, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to add users to selected groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to add users " + names + " to groups " + ids + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Users are added to selected groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Users " + names + " are added to groups " + ids );
      }
      
    } );
  }

  @Override
  public void confirm( String subject ) {
    if ( DELETE_GROUPS_SUBJECT.equals( subject ) ) {
      doDeleteGroups( );
    }
    
  }

  private void doDeleteGroups( ) {
    if ( currentSelected == null || currentSelected.size( ) < 1 ) {
      return;
    }
    
    final ArrayList<String> ids = Lists.newArrayList( ); 
    for ( SearchResultRow row : currentSelected ) {
      ids.add( row.getField( 0 ) );
    }
    
    clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.LOADING, "Deleting groups ...", 0 );
    
    clientFactory.getBackendService( ).deleteGroups( clientFactory.getLocalSession( ).getSession( ), ids, new AsyncCallback<Void>( ) {

      @Override
      public void onFailure( Throwable caught ) {
        ActivityUtil.logoutForInvalidSession( clientFactory, caught );
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.ERROR, "Failed to delete groups", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.ERROR, "Failed to delete groups " + ids + ": " + caught.getMessage( ) );
      }

      @Override
      public void onSuccess( Void arg0 ) {
        clientFactory.getShellView( ).getFooterView( ).showStatus( StatusType.NONE, "Groups deleted", FooterView.DEFAULT_STATUS_CLEAR_DELAY );
        clientFactory.getShellView( ).getLogView( ).log( LogType.INFO, "Groups " + ids + " deleted" );
        reloadCurrentRange( );
      }
      
    } );
  }
  
}
