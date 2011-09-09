package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.service.QuickLink;
import com.eucalyptus.webui.client.service.QuickLinkTag;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.CellTree.Resources;
import com.google.gwt.user.cellview.client.CellTree.Style;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;


public class DirectoryViewImpl extends Composite implements DirectoryView {
  
  public static final int TREENODE_SIZE = 50;
  
  private static final Logger LOG = Logger.getLogger( DirectoryViewImpl.class.getName( ) );
  
  private static DirectoryViewImplUiBinder uiBinder = GWT.create( DirectoryViewImplUiBinder.class );
  interface DirectoryViewImplUiBinder extends UiBinder<Widget, DirectoryViewImpl> {}
  
  public static interface TreeResources extends Resources {
    @Source( "CategoryTreeStyle.css" )
    Style cellTreeStyle( );
  }
  
  @UiField
  ScrollPanel treePanel;

  private Presenter presenter;
  
  private final SingleSelectionModel<QuickLink> selectionModel = new SingleSelectionModel<QuickLink>( QuickLink.KEY_PROVIDER );
  
  public DirectoryViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @Override
  public void buildTree( ArrayList<QuickLinkTag> data ) {
    if ( data == null || data.size( ) < 1 ) {
      LOG.log( Level.WARNING, "Can not build category tree: data is empty." );
      return;
    }
    
    selectionModel.addSelectionChangeHandler( new SelectionChangeEvent.Handler( ) {
      
      @Override
      public void onSelectionChange( SelectionChangeEvent event ) {
        QuickLink selected = selectionModel.getSelectedObject( );
        if ( selected != null ) {
          LOG.log( Level.INFO, "Selected: " + selected.getName( ) );
          presenter.switchQuickLink( selected.getQuery( ) );
        }
      }
    } );
    
    CellTree.Resources resource = GWT.create( TreeResources.class );
    
    CellTree tree = new CellTree( new QuickLinkTreeModel( data, selectionModel ), null, resource );
    tree.setKeyboardSelectionPolicy( KeyboardSelectionPolicy.DISABLED );
    tree.setAnimationEnabled( false );
    tree.setDefaultNodeSize( TREENODE_SIZE );
    openAllNode( tree.getRootTreeNode( ) );
    
    treePanel.clear( );
    treePanel.add( tree );
  }

  private void openAllNode( TreeNode root ) {
    for ( int i = 0; i < root.getChildCount( ); i++ ) {
      root.setChildOpen( i, true );
    }
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }

  @Override
  public void changeSelection( QuickLink link ) {
    if ( link != null ) {
      selectionModel.setSelected( link, true );
    } else {
      // deselect current
      selectionModel.setSelected( selectionModel.getSelectedObject( ), false );
    }
  }
}
