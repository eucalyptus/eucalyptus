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
package com.eucalyptus.ws.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.stages.HmacV2UserAuthenticationStage;
import com.eucalyptus.ws.stages.QueryBindingStage;
import com.eucalyptus.ws.stages.UnrollableStage;


public class EucalyptusQueryPipeline extends FilteredPipeline {
  private static Logger LOG = Logger.getLogger( EucalyptusQueryPipeline.class );

  @Override
  protected void addStages( List<UnrollableStage> stages ) {
    stages.add( new HmacV2UserAuthenticationStage( ) );
    stages.add( new QueryBindingStage( ) );
  }

  @Override
  protected boolean checkAccepts( HttpRequest message ) {
    if ( message instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) message;
      if ( httpRequest.getMethod( ).equals( HttpMethod.POST ) ) {
        Map<String,String> parameters = new HashMap<String,String>( httpRequest.getParameters( ) );
        ChannelBuffer buffer = httpRequest.getContent( );
        buffer.markReaderIndex( );
        byte[] read = new byte[buffer.readableBytes( )];
        buffer.readBytes( read );
        String query = new String( read );
        buffer.resetReaderIndex( );
        for ( String p : query.split( "&" ) ) {
          String[] splitParam = p.split( "=" );
          String lhs = splitParam[0];
          String rhs = splitParam.length == 2 ? splitParam[1] : null;
          parameters.put( lhs, rhs );
        }
        for ( RequiredQueryParams p : RequiredQueryParams.values( ) ) {
          if ( !parameters.containsKey( p.toString( ) ) ) {
            return false;
          }
        }
      } else {
        for ( RequiredQueryParams p : RequiredQueryParams.values( ) ) {
          if ( !httpRequest.getParameters( ).containsKey( p.toString( ) ) ) {
            return false;
          }
        }
      }
      return true && message.getUri( ).startsWith( "/services/Eucalyptus" );
    }
    return false;
  }

  @Override
  public String getPipelineName( ) {
    return "eucalyptus-query";
  }

  public enum RequiredQueryParams {
    SignatureVersion,
    Version
  }
  
  public enum OperationParameter {

    Operation, Action;
    private static String patterh = buildPattern();

    private static String buildPattern()
    {
      StringBuilder s = new StringBuilder();
      for ( OperationParameter op : OperationParameter.values() ) s.append( "(" ).append( op.name() ).append( ")|" );
      s.deleteCharAt( s.length() - 1 );
      return s.toString();
    }

    public static String toPattern()
    {
      return patterh;
    }

    public static String getParameter( Map<String,String> map )
    {
      for( OperationParameter op : OperationParameter.values() )
        if( map.containsKey( op.toString() ) )
          return map.get( op.toString() );
      return null;
    }
  }

}
