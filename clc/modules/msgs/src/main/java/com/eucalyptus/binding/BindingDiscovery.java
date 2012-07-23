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

package com.eucalyptus.binding;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class BindingDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( BindingDiscovery.class );

  public BindingDiscovery( ) {}

  @Override
  public Double getPriority( ) {
    return 0.9;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Exception {
    Field f;
    String bindingList;
    try {
      f = candidate.getDeclaredField( "JiBX_bindingList" );
      bindingList = ( String ) f.get( null );
    } catch ( Exception e ) {
      return false;
    }
    List<String> bindings = Lists.transform( Arrays.asList( bindingList.split( "\\|" ) ), new Function<String,String>() {
      @Override
      public String apply( String arg0 ) {
        return BindingManager.sanitizeNamespace( arg0.replaceAll(".*JiBX_","").replaceAll("Factory","") );
      }        
    });
    boolean seeded = false;
    for( String binding : bindings ) {
      if( binding.length( ) > 2 ) {
        try {
          seeded |= BindingManager.seedBinding( binding, candidate );
        } catch ( Exception e ) {
        }
      }
    }
    return seeded;
  }
  
}
