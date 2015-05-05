/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth;

import java.util.List;
import java.util.ServiceLoader;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 *
 */
public class Regions {

  private static Supplier<RegionProvider> regionProviderSupplier = serviceLoaderSupplier( RegionProvider.class );

  private static <T> Supplier<T> serviceLoaderSupplier( final Class<T> serviceClass ) {
    return Suppliers.memoize( new Supplier<T>() {
      @Override
      public T get() {
        return ServiceLoader.load( serviceClass ).iterator().next();
      }
    } );
  }

  private static RegionProvider getRegionProvider( ) {
    return regionProviderSupplier.get( );
  }

  public static List<RegionService> getRegionServicesByType( final String serviceType ) {
    return getRegionProvider( ).getRegionServicesByType( serviceType );
  }


  public interface RegionProvider {
    List<RegionService> getRegionServicesByType( String serviceType );
  }
}
