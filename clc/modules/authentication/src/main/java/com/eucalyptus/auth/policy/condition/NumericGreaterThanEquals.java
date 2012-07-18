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

@PolicyCondition( { Conditions.NUMERICGREATERTHANEQUALS, Conditions.NUMERICGREATERTHANEQUALS_S } )
public class NumericGreaterThanEquals implements NumericConditionOp {
  
  private static final Logger LOG = Logger.getLogger( NumericEquals.class );

  @Override
  public boolean check( String key, String value ) {
    try {
      return Integer.valueOf( key ).compareTo( Integer.valueOf( value ) ) >= 0;
    } catch ( NumberFormatException e ) {
      try {
        return Double.valueOf( key ).compareTo( Double.valueOf( value ) ) >= 0;
      } catch ( NumberFormatException e1 ) {
        // It does not make sense to check the equality of two floats.
        LOG.error( "Invalid number format", e1 );        
      }
    }
    return false;
  }
  
}
