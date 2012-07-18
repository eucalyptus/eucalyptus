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

package com.eucalyptus.cluster.callback;

import org.apache.log4j.Logger;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.util.async.SubjectMessageCallback;
import edu.ucsb.eucalyptus.msgs.DescribePublicAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribePublicAddressesType;

public class PublicAddressStateCallback extends SubjectMessageCallback<Cluster, DescribePublicAddressesType, DescribePublicAddressesResponseType> {
  private static Logger LOG = Logger.getLogger( PublicAddressStateCallback.class );
  
  public PublicAddressStateCallback( ) {
    super( ( DescribePublicAddressesType ) new DescribePublicAddressesType( ).regarding( ) );
  }
  
  @Override
  public void fire( DescribePublicAddressesResponseType reply ) {
    this.getSubject( ).getState( ).setPublicAddressing( true );
    try {
      Addresses.getAddressManager( ).update( this.getSubject( ), reply.getAddresses( ) );
    } catch ( Exception ex ) {
      LOG.error( ex );
    }
  }
  
  @Override
  public void fireException( Throwable t ) {
    if( t instanceof FailedRequestException ) {
      LOG.warn( "Response from cluster [" + this.getSubject( ).getName( ) + "]: " + t.getMessage( ) );
    } else {
      LOG.warn( "[" + this.getSubject( ).getName( ) + "]: " + t.getMessage( ) );
    }
    if( t instanceof FailedRequestException ) {
      this.getSubject( ).getState( ).setPublicAddressing( false );
    } else {
      LOG.error( t, t );
    }
  }
  
}
