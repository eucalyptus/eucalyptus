/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cassandra.config;

import static com.eucalyptus.bootstrap.Bootstrap.Stage.DatabaseInit;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.cassandra.common.Cassandra;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 *
 */
@RunDuring( DatabaseInit )
@Provides( Cassandra.class )
public class CassandraBootstrapper extends Bootstrapper {

  @Override
  public boolean check( ) throws Exception {
    return CassandraSysUtil.checkCassandra( );
  }

  @Override
  public void destroy( ) throws Exception {
  }

  @Override
  public boolean disable( ) throws Exception {
    return true;
  }

  @Override
  public boolean enable( ) throws Exception {
    return true;
  }

  @Override
  public boolean load( ) throws Exception {
    CassandraSysUtil.createDirectories( );
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    if ( !check( ) ) {
      throw new EucalyptusCloudException( "Cannot start cassandra (waiting for seed?)" );
    }
    CassandraSysUtil.createDirectories( );
    CassandraSysUtil.writeConfiguration( );
    CassandraSysUtil.startCassandra( );
    CassandraSysUtil.createKeyspaces( );
    return true;
  }

  @Override
  public boolean stop( ) throws Exception {
    CassandraSysUtil.stopCassandra( );
    return true;
  }
}
