/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.config;

import edu.ucsb.eucalyptus.transport.Axis2Connector;
import edu.ucsb.eucalyptus.transport.Axis2MessageReceiver;
import edu.ucsb.eucalyptus.transport.OverloadedWebserviceMethod;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;

import java.lang.reflect.Method;

public class Axis2ServiceBuilder
{
  public static AxisService getAxisService( Axis2MessageReceiver msgReceiver ) throws AxisFault
  {
    String className = msgReceiver.getProperties().getServiceClass().getCanonicalName();
    Axis2Connector connector = ( Axis2Connector ) msgReceiver.getConnector();
    AxisService axisService = AxisService.createService( className, connector.getAxisConfig().getAxisConfiguration() );
    axisService.setActive( true );

    axisService.setName( msgReceiver.getService().getName() );
    axisService.addParameter( new Parameter( org.apache.axis2.Constants.SERVICE_CLASS, className ) );

//    axisService.addParameter( new Parameter( "modifyUserWSDLPortAddress", Boolean.TRUE.toString() ) );
//    axisService.addParameter( new Parameter( "useOriginalWSDL", Boolean.TRUE.toString() ) );
    axisService.setClassLoader( ClassLoader.getSystemClassLoader() );

    Method[] methods = msgReceiver.getProperties().getServiceClass().getDeclaredMethods();
    for ( Method m : methods )
    {
      OverloadedWebserviceMethod annote = m.getAnnotation( OverloadedWebserviceMethod.class );
      if ( annote != null )
        for ( String annoteMethod : annote.actions() )
          axisService.addOperation( Axis2OperationBuilder.getAxisOperation( annoteMethod, msgReceiver ) );
      else
      {
        AxisOperation op = Axis2OperationBuilder.getAxisOperation( m.getName(), msgReceiver );
        axisService.addOperation( op );
        axisService.mapActionToOperation( m.getName(), op );
      }
    }
    return axisService;
  }
}
