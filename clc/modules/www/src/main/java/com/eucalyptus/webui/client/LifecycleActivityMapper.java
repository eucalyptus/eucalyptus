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

package com.eucalyptus.webui.client;

import com.eucalyptus.webui.client.activity.ApplyActivity;
import com.eucalyptus.webui.client.activity.ConfirmSignupActivity;
import com.eucalyptus.webui.client.activity.LoginActivity;
import com.eucalyptus.webui.client.activity.ResetPasswordActivity;
import com.eucalyptus.webui.client.activity.ShellActivity;
import com.eucalyptus.webui.client.place.ApplyPlace;
import com.eucalyptus.webui.client.place.ConfirmSignupPlace;
import com.eucalyptus.webui.client.place.LoginPlace;
import com.eucalyptus.webui.client.place.ResetPasswordPlace;
import com.eucalyptus.webui.client.place.ShellPlace;
import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

/**
 * Mapping the lifecycle activities and places.
 */
public class LifecycleActivityMapper implements ActivityMapper {
  
  private ClientFactory clientFactory;
  
  public LifecycleActivityMapper( ClientFactory clientFactory ) {
    this.clientFactory = clientFactory;
  }
  
  @Override
  public Activity getActivity( Place place ) {
    if ( place instanceof LoginPlace ) {
      return new LoginActivity( ( LoginPlace ) place, this.clientFactory );
    } else if ( place instanceof ShellPlace ) {
      return new ShellActivity( ( ShellPlace ) place, this.clientFactory );
    } else if ( place instanceof ApplyPlace ) {
      return new ApplyActivity( ( ApplyPlace ) place, this.clientFactory );
    } else if ( place instanceof ConfirmSignupPlace ) {
      return new ConfirmSignupActivity( ( ConfirmSignupPlace ) place, this.clientFactory );
    } else if ( place instanceof ResetPasswordPlace ) {
      return new ResetPasswordActivity( ( ResetPasswordPlace ) place, this.clientFactory );
    }
    return null;
  }
  
}
