/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.ws.client;

import java.lang.reflect.Modifier;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.google.common.collect.Maps;
import io.netty.channel.ChannelInitializer;

/**
 *
 */
public class ClientChannelInitializers {
  private static final Logger LOG = Logger.getLogger( ClientChannelInitializers.class );
  private static final Map<Class<?>, ChannelInitializer> CHANNEL_INITIALIZERS = Maps.newHashMap( );


  /**
   * Returns the ChannelPipelineFactory for the {@code compId} else {@code null} if none was discovered.
   */
  public static ChannelInitializer lookup( Class<?> compId ) {
    return CHANNEL_INITIALIZERS.get( compId );
  }

  /**
   * Discovers and registers ChannelInitializer instances for discovered components.
   */
  public static class ChannelInitializerDiscovery extends ServiceJarDiscovery {

    @SuppressWarnings( { "rawtypes", "unchecked", "synthetic-access" } )
    @Override
    public boolean processClass( final Class candidate ) throws Exception {
      if ( ChannelInitializer.class.isAssignableFrom( candidate ) &&
          !Modifier.isAbstract( candidate.getModifiers( ) ) &&
          Modifier.isPublic( candidate.getModifiers( ) ) &&
          Ats.from( candidate ).has( ComponentPart.class ) ) {
        try {
          final Class<? extends ComponentId> compIdClass = Ats.from( candidate ).get( ComponentPart.class ).value( );
          final ChannelInitializer channelInitializer =
              ChannelInitializer.class.cast( Classes.newInstance( candidate ) );
          CHANNEL_INITIALIZERS.put( compIdClass, channelInitializer );
          return true;
        } catch ( final Exception ex ) {
          LOG.error( "Error in client channel initializer discovery for " + candidate, ex );
          return false;
        }

      } else {
        return false;
      }
    }

    @Override
    public Double getPriority( ) {
      return 0.1d;
    }
  }
}
