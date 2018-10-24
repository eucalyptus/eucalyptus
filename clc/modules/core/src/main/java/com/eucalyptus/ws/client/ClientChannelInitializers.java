/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
