/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.network.applicator;

import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.NetworkInfo;
import com.eucalyptus.util.TypedKey;

/**
 *
 */
class MarshallingApplicatorHelper {

  private static final Logger logger = Logger.getLogger( MarshallingApplicatorHelper.class );

  private static final TypedKey<String> MARSHALLED_INFO_KEY = TypedKey.create( "MarshalledNetworkInfo" );

  static void clearMarshalledNetworkInfoCache( final ApplicatorContext context ) {
    context.removeAttribute( MARSHALLED_INFO_KEY );
  }

  static String getMarshalledNetworkInfo( final ApplicatorContext context ) throws ApplicatorException {
    String networkInfo = context.getAttribute( MARSHALLED_INFO_KEY );
    if ( networkInfo == null ) try {
      final NetworkInfo info = context.getNetworkInfo( );
      final JAXBContext jc = JAXBContext.newInstance( "com.eucalyptus.cluster" );
      final StringWriter writer = new StringWriter( 8192 );
      jc.createMarshaller().marshal( info, writer );

      networkInfo = writer.toString( );
      if ( logger.isTraceEnabled( ) ) {
        logger.trace( "Broadcasting network information:\n${networkInfo}" );
      }
      context.setAttribute( MARSHALLED_INFO_KEY, networkInfo );
    } catch ( final JAXBException e ) {
      throw new ApplicatorException( "Error marshalling network information", e );
    }
    return networkInfo;
  }

}
