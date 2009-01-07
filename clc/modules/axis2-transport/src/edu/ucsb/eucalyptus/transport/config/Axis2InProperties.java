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

import edu.ucsb.eucalyptus.transport.auth.Authenticator;
import org.apache.axis2.context.ConfigurationContext;

import java.util.Map;

public class Axis2InProperties extends Axis2Properties {

  //:: used for mapping authentication info onto messages :://
  private Authenticator authenticator;

  //:: other :://
  private Class serviceClass;
  private String wsdl;
  private boolean httpQuerySupport;

  public Axis2InProperties( final Class serviceClass, final Map<String, String> props, final ConfigurationContext ctx )
  {
    super( props, ctx );
    this.serviceClass = serviceClass;
    this.authenticator = ( Authenticator ) Key.AUTHENTICATOR.getNewInstance( props );
    this.wsdl = Key.WSDL.getString( props );
    this.httpQuerySupport = Key.HTTP_QUERY_SUPPORT.getBoolean( props );
  }

  public Authenticator getAuthenticator()
  {
    return authenticator;
  }

  public Class getServiceClass()
  {
    return serviceClass;
  }

  public String getWsdl()
  {
    return wsdl;
  }

  public boolean isHttpQuerySupport()
  {
    return httpQuerySupport;
  }
}
