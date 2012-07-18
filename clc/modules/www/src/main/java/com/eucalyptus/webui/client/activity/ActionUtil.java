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

package com.eucalyptus.webui.client.activity;

import com.google.common.base.Strings;

public class ActionUtil {
  
  public static WebAction parseAction( String action ) {
    if ( Strings.isNullOrEmpty( action ) ) {
      return null;
    }
    final String[] parts = action.split( WebAction.ACTION_SEPARATOR, 2 );
    if ( parts.length < 2 || Strings.isNullOrEmpty( parts[0] ) || Strings.isNullOrEmpty( parts[1] ) ) {
      return null;
    }
    final String[] subParts = parts[1].split( WebAction.KEY_VALUE_SEPARATOR, 2 );
    return new WebAction( ) {

      @Override
      public String getAction( ) {
        return parts[0];
      }

      @Override
      public String getValue( String key ) {
        if ( key != null && key.equals( subParts[0] ) ) {
          return subParts[1];
        }
        return null;
      }
      
    };
  }
  
}
