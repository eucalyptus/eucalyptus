package com.eucalyptus.webui.client.view;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import com.google.gwt.view.client.TreeViewModel.NodeInfo;

public class CategoryTree extends Composite {
  
  private static Logger LOG = Logger.getLogger( "CategoryTree" );

  private static class CustomTreeModel implements TreeViewModel {

    public <T> NodeInfo<?> getNodeInfo(T value) {
      ListDataProvider<String> dataProvider = new ListDataProvider<String>();
      for (int i = 0; i < 2; i++) {
        dataProvider.getList().add(value + "." + String.valueOf(i));
      }
      return new DefaultNodeInfo<String>(dataProvider, new TextCell());
    }

    public boolean isLeaf(Object value) {
      return value.toString().length() > 10;
    }
  }
  
  private CellTree tree;
  
  public CategoryTree( ) {
    TreeViewModel model = new CustomTreeModel( );
    this.tree = new CellTree( model, "root" );
    
    initWidget( this.tree );    
  }
  
}
