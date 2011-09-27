/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.server;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentPart;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

public class Pipelines {
  private static final Logger                                  LOG                = Logger.getLogger( Pipelines.class );
  private static final Multimap<ComponentId, FilteredPipeline> componentPipelines = TreeMultimap.create( );
  private static final Set<FilteredPipeline>                   pipelines          = new ConcurrentSkipListSet<FilteredPipeline>( );
  
  static void register( final FilteredPipeline pipeline ) {
    LOG.info( "-> Registering pipeline: " + pipeline );
    pipelines.add( pipeline );
  }
  
  static void deregister( final FilteredPipeline pipeline ) {
    LOG.info( "-> Deregistering pipeline: " + pipeline );
    pipelines.remove( pipeline );
  }
  
  static FilteredPipeline find( final HttpRequest request ) throws DuplicatePipelineException, NoAcceptingPipelineException {
    FilteredPipeline candidate = null;
    for ( FilteredPipeline f : pipelines ) {
      if ( f.checkAccepts( request ) ) {
        
        if ( candidate != null ) {
          EventRecord.here( Pipelines.class, EventType.PIPELINE_DUPLICATE, f.toString( ) ).debug( );
        } else {
          candidate = f;
        }
      }
    }
    if ( candidate == null ) {
      for ( FilteredPipeline f : componentPipelines.values( ) ) {
        if ( f.checkAccepts( request ) ) {
          candidate = f;
        }
      }
    }
    if ( candidate == null ) {
      if ( Logs.isExtrrreeeme( ) ) {
        if ( request instanceof MappingHttpMessage ) {
          ( ( MappingHttpMessage ) request ).logMessage( );
          for ( FilteredPipeline p : pipelines ) {
            LOG.debug( "PIPELINE: " + p );
          }
        }
      }
      throw new NoAcceptingPipelineException( );
    }
    if ( Logs.isExtrrreeeme( ) ) {
      EventRecord.here( Pipelines.class, EventType.PIPELINE_UNROLL, candidate.toString( ) ).debug( );
    }
    return candidate;
  }
  
  public static boolean enable( ComponentId compId ) {
    LOG.info( "-> Registering component pipeline: " + compId.getName( ) + " " + componentPipelines.get( compId ) );
    return true; //pipelines.addAll( componentPipelines.get( compId ) );
  }
  
  public static boolean disable( ComponentId compId ) {
    for ( FilteredPipeline pipeline : componentPipelines.get( compId ) ) {
      LOG.info( "-> Deregistering pipeline: " + compId.getName( ) + " " + pipeline );
    }
    return true; //pipelines.removeAll( componentPipelines.get( compId ) );
  }
  
  public static class PipelineDiscovery extends ServiceJarDiscovery {
    
    @SuppressWarnings( { "rawtypes", "unchecked", "synthetic-access" } )
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( FilteredPipeline.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) )
           && !Modifier.isInterface( candidate.getModifiers( ) ) && Ats.from( candidate ).has( ComponentPart.class ) ) {
        try {
          ComponentId compId = ( ComponentId ) Ats.from( candidate ).get( ComponentPart.class ).value( ).newInstance( );
          Class<? extends FilteredPipeline> pipelineClass = candidate;
          FilteredPipeline pipeline = Classes.newInstance( pipelineClass );
          Pipelines.componentPipelines.put( compId, pipeline );
          Pipelines.pipelines.add( pipeline );
          return true;
        } catch ( Exception ex ) {
          LOG.trace( ex, ex );
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
