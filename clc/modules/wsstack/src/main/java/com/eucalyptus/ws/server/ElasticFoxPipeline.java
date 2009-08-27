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
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.server;

import java.util.List;

import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.stages.ElasticFoxMangleStage;
import com.eucalyptus.ws.stages.UnrollableStage;

public class ElasticFoxPipeline extends EucalyptusQueryPipeline {

  @Override
  protected void addStages( List<UnrollableStage> stages ) {
    super.addStages( stages );
    stages.add( new ElasticFoxMangleStage( ) );
  }

  @Override
  protected boolean checkAccepts( HttpRequest message ) {
    if ( message instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) message;
      String userAgent = httpRequest.getHeader( "User-Agent" );
      if( userAgent != null && userAgent.matches( ".*Elasticfox.*" )){
        httpRequest.setServicePath( httpRequest.getServicePath( ).replaceAll( "Eucalyptus/", "Eucalyptus" ) );
        return true;
      }
    }
    return false;
  }

  @Override
  public String getPipelineName( ) {
    return "elasticfox-"+super.getPipelineName( );
  }

  
  
}
