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

import java.util.List;

import org.jboss.netty.handler.codec.http.HttpRequest;

import com.eucalyptus.ws.client.NioMessageReceiver;
import com.eucalyptus.ws.stages.BindingStage;
import com.eucalyptus.ws.stages.SoapInternalAuthenticationStage;
import com.eucalyptus.ws.stages.UnrollableStage;

public class InternalSoapPipeline extends FilteredPipeline {

  private String servicePath;
  private String serviceName;
  
  public InternalSoapPipeline( NioMessageReceiver msgReceiver, String serviceName, String servicePath ) {
    super( msgReceiver );
    this.servicePath = servicePath;
    this.serviceName = serviceName;
  }

  @Override
  protected void addStages( List<UnrollableStage> stages ) {
    stages.add( new SoapInternalAuthenticationStage( ) );
    stages.add( new BindingStage( ) );
  }

  @Override
  protected boolean checkAccepts( HttpRequest message ) {
    return message.getUri( ).endsWith( servicePath ) && message.getHeaderNames().contains( "SOAPAction" );
  }

  @Override
  public String getPipelineName( ) {
    return "internal-pipeline-" + this.serviceName;
  }

}
