/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.policy;

import java.util.regex.Pattern;
import com.eucalyptus.auth.policy.key.Key;
import net.sf.json.JSONException;

/**
 *
 */
public interface ComputeKey extends Key {
  static class Validation {
    private static final Pattern arnPattern = Pattern.compile( "[arn*?]{1,64}:[aws*?]{1,64}:[a-zA-Z0-9*?-]{1,64}(:[a-zA-Z0-9*?-]{0,64}(:(|amazon|[0-9*?]{1,12})(:.{1,2048})?)?)?" );

    static void assertArnValue( final String value ) throws JSONException {
      if ( !arnPattern.matcher( value ).matches( ) ) {
        throw new JSONException( "Invalid ARN: " + value );
      }
    }
  }
}
