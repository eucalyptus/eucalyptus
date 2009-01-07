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

import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

public class EucalyptusQuerySecurityHandler extends HMACQuerySecurityHandler {

  private static Logger LOG = Logger.getLogger( EucalyptusQuerySecurityHandler.class );

  public static SimpleDateFormat[] iso8601 = {
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'Z" )
  };

  public QuerySecurityHandler getInstance()
  {
    return new EucalyptusQuerySecurityHandler();
  }

  public String getName()
  {
    return "1";
  }

  public UserInfo authenticate( final HttpRequest httpRequest ) throws QuerySecurityException
  {
    return handle( httpRequest.getServicePath(), httpRequest.getHttpMethod(), httpRequest.getParameters(), httpRequest.getHeaders() );
  }

  public UserInfo handle(String addr, String verb, Map<String, String> parameters, Map<String, String> headers ) throws QuerySecurityException
  {
    this.checkParameters( parameters );

    //:: check the signature :://
    String sig = parameters.remove( SecurityParameter.Signature.toString() );
    String queryId = parameters.get( SecurityParameter.AWSAccessKeyId.toString() );
    String queryKey = findQueryKey( queryId );

    String paramString = makeSubjectString( parameters );
    String paramString2 = makePlusSubjectString( parameters );

    String authSig = checkSignature( queryKey, paramString );
    String authSig2 = checkSignature( queryKey, paramString2 );
    if ( !authSig.equals( sig ) && !authSig2.equals( sig ) )
      throw new QuerySecurityException( "User authentication failed." );

    //:: check the timestamp :://
    Calendar now = Calendar.getInstance();
    Calendar expires = null;
    if ( parameters.containsKey( SecurityParameter.Timestamp.toString() ) )
    {
      String timestamp = parameters.remove( SecurityParameter.Timestamp.toString() );
      expires = parseTimestamp( timestamp );
      expires.add( Calendar.MINUTE, 5 );
    }
    else
    {
      String exp = parameters.remove( SecurityParameter.Expires.toString() );
      expires = parseTimestamp( exp );
    }
    if ( now.after( expires ) )
      throw new QuerySecurityException( "Message has expired." );

    for ( Axis2QueryDispatcher.OperationParameter op : Axis2QueryDispatcher.OperationParameter.values() ) parameters.remove( op.name() );
    parameters.remove( Axis2QueryDispatcher.RequiredQueryParams.SignatureVersion.toString() );
    parameters.remove( Axis2QueryDispatcher.RequiredQueryParams.Version.toString() );

    return findUserId( parameters.remove( SecurityParameter.AWSAccessKeyId.toString() ) );
  }

  private void checkParameters( final Map<String, String> parameters ) throws QuerySecurityException
  {
    if ( !parameters.containsKey( SecurityParameter.AWSAccessKeyId.toString() ) )
      throw new QuerySecurityException( "Missing required parameter: " + SecurityParameter.AWSAccessKeyId );
    if ( !parameters.containsKey( SecurityParameter.Signature.toString() ) )
      throw new QuerySecurityException( "Missing required parameter: " + SecurityParameter.Signature );
    if ( !parameters.containsKey( SecurityParameter.Timestamp.toString() )
         && !parameters.containsKey( SecurityParameter.Expires.toString() ) )
      throw new QuerySecurityException( "One of the following parameters must be specified: " + SecurityParameter.Timestamp + " OR " + SecurityParameter.Expires );
  }

}