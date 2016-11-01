/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/

package com.eucalyptus.simplequeue.common.policy;

import com.eucalyptus.auth.policy.ern.ResourceNameSupport;
import com.google.common.base.Strings;

/**
 *
 */
public class SimpleQueueResourceName extends ResourceNameSupport {

  public SimpleQueueResourceName(String region, String account, String id) {
    super(SimpleQueuePolicySpec.VENDOR_SIMPLEQUEUE, region, account, SimpleQueuePolicySpec.RESOURCE_TYPE, id);
  }

  @Override
  public String toString( ) {
    return new StringBuilder( )
      .append( ARN_PREFIX )
      .append( getService( ) ).append( ':' )
      .append( Strings.nullToEmpty(getRegion()) ).append( ':' )
      .append( Strings.nullToEmpty( getAccount( ) ) ).append( ':' )
      .append( getType( ).equals(SimpleQueuePolicySpec.RESOURCE_TYPE)  ? "" : getType() + "/" )
      .append( getResourceName( ) ).toString( );

  }
}