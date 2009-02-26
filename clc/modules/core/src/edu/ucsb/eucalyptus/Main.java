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

package edu.ucsb.eucalyptus;

import edu.ucsb.eucalyptus.ic.HttpServer;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.*;
import org.apache.log4j.Logger;
import org.mule.MuleServer;

public class Main {

  private static Logger LOG = Logger.getLogger( Main.class );

  public static void main( String[] args ) throws Exception
  {
    StartupChecks.doChecks();

    MuleServer server = new MuleServer( "eucalyptus-mule.xml" );
    server.start( false, true );

    Messaging.dispatch( HttpServer.REF, "hi" );

    DescribeAvailabilityZonesType descAz = new DescribeAvailabilityZonesType();
    descAz.setUserId( EucalyptusProperties.NAME );
    descAz.setEffectiveUserId( EucalyptusProperties.NAME );
    descAz.setCorrelationId( "" );
    try {
      Messaging.send( WalrusProperties.WALRUS_REF, new InitializeWalrusType() );
    } catch (Exception e) {} 
    try {
      if( System.getProperty("euca.ebs.disable") == null )
        Messaging.send( StorageProperties.STORAGE_REF, new InitializeStorageManagerType() );
    } catch (Exception e) {} 

    try {
      Messaging.dispatch( "vm://RequestQueue", descAz );
    } catch (Exception e) {} 

    LOG.info( "Eucalyptus started." );
  }
}
