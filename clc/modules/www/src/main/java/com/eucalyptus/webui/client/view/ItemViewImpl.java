/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
