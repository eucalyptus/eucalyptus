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
package com.eucalyptus.ws.handlers;

import java.util.List;
import java.util.Map;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.CollectionUtils;
import com.google.common.base.Function;

/**
 * Utility functionality for handlers that work with HMAC Signatures
 */
class SignatureHandlerUtils {

  static Function<String,List<String>> headerLookup( final MappingHttpRequest request ) {
    return new Function<String,List<String>>(){
      @Override
      public List<String> apply( final String header ) {
        return request.getHeaders( header );
      }
    };   
  }

  static Function<String,List<String>> parameterLookup( final MappingHttpRequest request ) {
    final Map<String,String> parameters = request.getParameters();
    return new Function<String,List<String>>(){
      @Override
      public List<String> apply( final String header ) {
        return CollectionUtils.<String>listUnit().apply( parameters.get( header ) );
      }
    };
  }
}
