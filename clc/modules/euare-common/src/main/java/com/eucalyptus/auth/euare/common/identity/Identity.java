/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.euare.common.identity;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.component.annotation.PublicService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;

/**
 *
 */
@PublicService
@PolicyVendor( "euid" )
@Partition( value = Identity.class, manyToOne=true )
@Description( "Eucalyptus identity service" )
public class Identity extends ComponentId {
  private static final long serialVersionUID = 1L;

  @Override
  public boolean isUseServiceHostName( ) {
    return true;
  }

  @Override
  public Bootstrap getClientBootstrap( ) {
    final Bootstrap clientBootstrap = super.getClientBootstrap( );
    return clientBootstrap
        .option( ChannelOption.CONNECT_TIMEOUT_MILLIS, 6000 );
  }
}
