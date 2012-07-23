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

package com.eucalyptus.address;

import java.util.List;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.vm.VmInstance;
import com.google.common.collect.Lists;

public class NullSystemAddressManager extends AbstractSystemAddressManager {
  
  @Override
  public List<Address> allocateSystemAddresses( Partition partition, int count ) throws NotEnoughResourcesException {
    throw new RuntimeException( "The system is not configured to support public addresses." );
    //TODO: add some output to help figure out why.
  }
  
  @Override
  public void assignSystemAddress( VmInstance vm ) {
    throw new RuntimeException( "The system is not configured to support public addresses." );
    //TODO: add some output to help figure out why.
  }
  
  @Override
  public List<Address> getReservedAddresses( ) {
    return Lists.newArrayList( );
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public void inheritReservedAddresses( List<Address> previouslyReservedAddresses ) {
    for( final Address addr : previouslyReservedAddresses ) {
      Addresses.release( addr );
    }
  }

}
