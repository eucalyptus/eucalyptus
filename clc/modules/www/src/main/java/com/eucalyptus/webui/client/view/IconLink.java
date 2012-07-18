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
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class IconLink extends Composite {
  
  private static IconLinkUiBinder uiBinder = GWT.create( IconLinkUiBinder.class );
  
  interface IconLinkUiBinder extends UiBinder<Widget, IconLink> {}
  
  interface TypeStyle extends CssResource {
    String anchor( );
    
    String link( );
    String service( );
    String user( );
    String account( );
    String group( );
    String image( );
    String vmtype( );
    String report( );
    String key( );
  }

  @UiField
  TypeStyle typeStyle;
  
  @UiField
  SpanElement icon;
  
  @UiField
  SpanElement title;
  
  @UiField
  Anchor anchor;
  
  public IconLink( String title, String href, String icon ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    this.title.setInnerText( title );
    this.anchor.setHref( href );
    this.icon.setClassName( getIcon( icon ) );
  }

  private String getIcon( String iconName ) {
    if ( "service".equals( iconName ) ) {
      return typeStyle.service( );
    } else if ( "user".equals( iconName ) ) {
      return typeStyle.user( );
    } else if ( "account".equals( iconName ) ) {
      return typeStyle.account( );
    } else if ( "group".equals( iconName ) ) {
      return typeStyle.group( );
    } else if ( "key".equals( iconName ) ) {
      return typeStyle.key( );      
    } else if ( "image".equals( iconName ) ) {
      return typeStyle.image( );
    } else if ( "vmtype".equals( iconName ) ) {
      return typeStyle.vmtype( );      
    } else if ( "report".equals( iconName ) ) {
      return typeStyle.report( );      
    }
    return typeStyle.link( );
  }
  
}
