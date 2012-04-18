package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import com.eucalyptus.webui.client.service.GuideItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

public class ItemViewImpl extends Composite implements ItemView {
  
  private static ItemViewImplUiBinder uiBinder = GWT.create( ItemViewImplUiBinder.class );
  
  interface ItemViewImplUiBinder extends UiBinder<Widget, ItemViewImpl> {}
  
  interface ItemStyle extends CssResource {
    String item( );
  }
  
  @UiField
  FlowPanel content;
  
  @UiField
  ItemStyle itemStyle;
  
  public ItemViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @Override
  public void display( ArrayList<GuideItem> items ) {
    if ( items != null ) {
      content.clear( );
      for ( GuideItem item : items ) {
        Widget itemWidget = new IconLink( item.getTitle( ), item.getLink( ), item.getIcon( ) );
        itemWidget.setStyleName( itemStyle.item( ) );
        content.add( itemWidget );
      }
    }
  }
  
}
