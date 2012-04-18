package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import com.eucalyptus.webui.client.service.DownloadInfo;
import com.eucalyptus.webui.client.view.InputViewImpl.Resources;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class DownloadViewImpl extends Composite implements DownloadView {
  
  private static DownloadViewImplUiBinder uiBinder = GWT.create( DownloadViewImplUiBinder.class );
  
  interface DownloadViewImplUiBinder extends UiBinder<Widget, DownloadViewImpl> {}
  
  interface GridStyle extends CssResource {
    String grid( );
  }
  
  interface Resources extends ClientBundle {
    @Source( "image/arrow_down_alt1_12x12_gray.png" )
    ImageResource download( );
  }
  
  public static final String ICON_COL_WIDTH = "32px";
  public static final String NAME_COL_WIDTH = "320px";
  
  @UiField
  GridStyle gridStyle;
  
  @UiField
  SimplePanel imagePanel;
  
  @UiField
  SimplePanel toolPanel;
  
  Resources resources;
  
  public DownloadViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    resources = GWT.create( Resources.class );
  }

  @Override
  public void displayImageDownloads( ArrayList<DownloadInfo> downloads ) {
    display( imagePanel, downloads );
  }

  @Override
  public void displayToolDownloads( ArrayList<DownloadInfo> downloads ) {
    display( toolPanel, downloads );
  }

  private void display( SimplePanel container, ArrayList<DownloadInfo> downloads ) {
    container.clear( );
    if ( downloads != null && downloads.size( ) > 0 ) {
      Grid grid = new Grid( downloads.size( ), 3 );
      grid.addStyleName( gridStyle.grid( ) );
      grid.getColumnFormatter( ).setWidth( 0, ICON_COL_WIDTH );
      grid.getColumnFormatter( ).setWidth( 1, NAME_COL_WIDTH );
      int row = 0;
      for ( DownloadInfo item : downloads ) {
        grid.setWidget( row, 0, new Image( resources.download( ) ) );
        grid.setWidget( row, 1, new Anchor( item.getName( ), item.getUrl( ) ) );
        grid.setText( row, 2, item.getDescription( ) );
        row++;
      }
      container.setWidget( grid );
    }
  }

}
