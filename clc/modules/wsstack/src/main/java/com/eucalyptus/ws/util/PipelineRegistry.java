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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package com.eucalyptus.ws.util;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.server.DuplicatePipelineException;
import com.eucalyptus.ws.server.FilteredPipeline;
import com.eucalyptus.ws.server.NoAcceptingPipelineException;

public class PipelineRegistry {
  private static PipelineRegistry registry;
  private static Logger           LOG = Logger.getLogger( PipelineRegistry.class );

  public static PipelineRegistry getInstance( ) {
    synchronized ( PipelineRegistry.class ) {
      if ( PipelineRegistry.registry == null ) {
        PipelineRegistry.registry = new PipelineRegistry( );
      }
    }
    return PipelineRegistry.registry;
  }

  private final NavigableSet<FilteredPipeline> pipelines = new ConcurrentSkipListSet<FilteredPipeline>( );

  public void register( final FilteredPipeline pipeline ) {
    LOG.info( "Registering pipeline: " + pipeline.getPipelineName( ) );
    this.pipelines.add( pipeline );
  }

  public FilteredPipeline find( final HttpRequest request ) throws DuplicatePipelineException, NoAcceptingPipelineException {
    FilteredPipeline candidate = null;
    for ( FilteredPipeline f : this.pipelines ) {
      if ( f.accepts( request ) ) {
        if ( candidate != null ) {
          LOG.warn( "More than one candidate pipeline.  Ignoring offer by: " + f.getPipelineName( ) + " of type " + f.getClass( ).getSimpleName( ) );
        } else {
          candidate = f;
        }
      }
    }
    if ( candidate == null ) { throw new NoAcceptingPipelineException( ); }
    return candidate;
  }

}
