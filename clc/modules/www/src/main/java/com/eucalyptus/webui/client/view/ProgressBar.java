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
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ProgressBar extends Composite {
  
  private static ProgressBarUiBinder uiBinder = GWT.create( ProgressBarUiBinder.class );
  
  interface ProgressBarUiBinder extends UiBinder<Widget, ProgressBar> {}
  
  @UiField
  DivElement bar;
  
  @UiField
  DivElement text;
  
  public ProgressBar( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  public void setProgress( int percent ) {
    String percentage = percent + "%";
    bar.setAttribute( "style", "width:" + percentage );
    text.setInnerText( percentage );
  }

}
