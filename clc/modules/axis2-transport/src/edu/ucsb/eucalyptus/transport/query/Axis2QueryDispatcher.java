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

package edu.ucsb.eucalyptus.transport.query;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.log4j.Logger;

import java.util.Map;

public class Axis2QueryDispatcher extends GenericHttpDispatcher implements RESTfulDispatcher {

  public static final String NAME = "Axis2QueryDispatcher";
  private static Logger LOG = Logger.getLogger( Axis2QueryDispatcher.class );

  public boolean accepts( final HttpRequest httpRequest, final MessageContext messageContext )
  {
    //:: decide about whether or not to accept the request for processing :://
    if( httpRequest == null || httpRequest.getParameters().isEmpty() ) return false;
    for( Axis2QueryDispatcher.RequiredQueryParams p : Axis2QueryDispatcher.RequiredQueryParams.values() )
      if( !httpRequest.getParameters().containsKey( p.toString() ) ) return false;
    if( OperationParameter.getParameter( httpRequest.getParameters() ) == null ) return false;
    return true;
  }

  public String getOperation( HttpRequest httpRequest, MessageContext messageContext )
  {
    return OperationParameter.getParameter( httpRequest.getParameters() );
  }

  public QuerySecurityHandler getSecurityHandler()
  {
    return new EucalyptusQuerySecurityHandler();
  }

  public QueryBinding getBinding()
  {
    return new DefaultQueryBinding();
  }

  public String getNamespace()
  {
    return "http://ec2.amazonaws.com/doc/"+this.getBinding().getName()+"/";
  }

  public void initDispatcher()
  {
    init( new HandlerDescription( NAME ) );
  }

  public enum RequiredQueryParams {
    SignatureVersion, Version
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
