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

package com.eucalyptus.auth.policy.condition;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.policy.key.Cidr;
import com.eucalyptus.auth.policy.key.CidrParseException;

@PolicyCondition( { Conditions.IPADDRESS } )
public class IpAddress implements AddressConditionOp {
  
  private static final Logger LOG = Logger.getLogger( IpAddress.class );
                                                     
  @Override
  public boolean check( String key, String value ) {
    try {
      return Cidr.valueOf( value ).matchIp( key );
    } catch ( CidrParseException e ) {
      LOG.error( "Invalid IP address and CIDR: " + key + ", " + value, e );
      return false;
    }
  }
  
}
