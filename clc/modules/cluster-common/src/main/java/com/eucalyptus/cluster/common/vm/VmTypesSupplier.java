/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.cluster.common.vm;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;

/**
 *
 */
public class VmTypesSupplier implements Supplier<Set<VmType>> {

  private static AtomicReference<Supplier<Set<VmType>>> supplierRef = new AtomicReference<>( Collections::emptySet );

  @Override
  public Set<VmType> get( ) {
    return supplierRef.get( ).get( );
  }

  public static void init( final Supplier<Set<VmType>> supplier ) {
    supplierRef.set( supplier );
  }
}
