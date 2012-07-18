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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class IconButton extends Composite implements HasClickHandlers {
  
  public enum Type {
    add,
    remove,
    modify,
    show
  }
  
  interface TypeStyle extends CssResource {
    String button( );
    // types
    String add( );
    String remove( );
    String modify( );
    String show( );
  }
  
  private static IconButtonUiBinder uiBinder = GWT.create( IconButtonUiBinder.class );
  
  interface IconButtonUiBinder extends UiBinder<Widget, IconButton> {}
  
  @UiField
  Anchor button;
  
  @UiField
  SpanElement icon;
  
  @UiField
  TypeStyle type;
  
  public IconButton( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  public void setType( Type t ) {
    switch ( t ) {
      case add:
        this.icon.setClassName( type.add( ) );
        break;
      case remove:
        this.icon.setClassName( type.remove( ) );
        break;
      case modify:
        this.icon.setClassName( type.modify( ) );
        break;
      case show:
        this.icon.setClassName( type.show( ) );
        break;        
    }
  }

  public void setHref( String url ) {
    this.button.setHref( url );
  }
  
  public String getHref( ) {
    return this.button.getHref( );
  }
  
  @Override
  public HandlerRegistration addClickHandler( ClickHandler handler ) {
    return this.button.addClickHandler( handler );
  }
  
}
