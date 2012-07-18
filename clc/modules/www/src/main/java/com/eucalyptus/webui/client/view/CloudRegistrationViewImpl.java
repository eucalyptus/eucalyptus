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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class CloudRegistrationViewImpl extends Composite implements CloudRegistrationView {
  
  private static CloudRegistrationViewImplUiBinder uiBinder = GWT.create( CloudRegistrationViewImplUiBinder.class );
  
  interface CloudRegistrationViewImplUiBinder extends UiBinder<Widget, CloudRegistrationViewImpl> {}
  
  @UiField
  Label cloudUrl;
  
  @UiField
  Label cloudId;
    
  public CloudRegistrationViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @Override
  public void display( String cloudUrl, String cloudId ) {
    this.cloudUrl.setText( cloudUrl );
    this.cloudId.setText( cloudId );
  }
  
}
