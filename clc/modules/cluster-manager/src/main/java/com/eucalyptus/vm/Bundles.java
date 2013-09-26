/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.vm;

import java.util.Map;
import java.util.NavigableSet;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.objectstorage.Walrus;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketType;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.MessageCallback;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;


public class Bundles {
  public static synchronized void putPreviousTask(VmBundleTask previousTask) {
    if (previousTask != null) {
      previousBundleTaskMap.put(previousTask.getBundleId(), VmBundleTask.copyOf(previousTask));
    } 
  }
  
  public static synchronized Map<String, VmBundleTask> getPreviousBundleTasks() {
    return ImmutableMap.copyOf(previousBundleTaskMap);
  }

  private static final Map<String, VmBundleTask> previousBundleTaskMap = Maps.newConcurrentMap();
  private static Logger LOG = Logger.getLogger( Bundles.class );
  
  public static MessageCallback createCallback( BundleInstanceType request ) throws AuthException, IllegalContextAccessException, ServiceStateException {
    final String walrusUrl = ServiceUris.remote( Topology.lookup( Walrus.class ) ).toASCIIString( );
    request.setUrl( walrusUrl );
    request.setAwsAccessKeyId( Accounts.getFirstActiveAccessKeyId( Contexts.lookup( ).getUser( ) ) );
    return new BundleCallback( request );
  }
  
  public static MessageCallback cancelCallback( CancelBundleTaskType request ) {
    return new CancelBundleCallback( request );
  }
  
  public static class CancelBundleCallback extends MessageCallback<CancelBundleTaskType, CancelBundleTaskResponseType> {
    private CancelBundleCallback( CancelBundleTaskType request ) {
      super( request );
    }
    
    @Override
    public void fire( CancelBundleTaskResponseType reply ) {
      if ( !reply.get_return( ) ) {
        LOG.info( "Attempt to CancelBundleTask for instance " + this.getRequest( ).getBundleId( ) + " has failed." );
      } else {
        EntityTransaction db = Entities.get( VmInstance.class );
        try {
          VmInstance vm = VmInstances.lookupByBundleId( this.getRequest( ).getBundleId( ) );
          vm.getRuntimeState( ).cancelBundleTask( );
          EventRecord.here( CancelBundleCallback.class, EventType.BUNDLE_CANCELLED, this.getRequest( ).toSimpleString( ), vm.getRuntimeState( ).getBundleTask( ).getBundleId( ),
                            vm.getInstanceId( ) ).info( );
          db.commit( );
        } catch ( Exception ex ) {
          Logs.exhaust( ).error( ex, ex );
          db.rollback( );
        }
      }
    }
  }
  
  public static MessageCallback bundleRestartInstanceCallback( BundleRestartInstanceType request ) {
    return new BundleRestartInstanceCallback( request );
  }
  
  public static class BundleRestartInstanceCallback extends MessageCallback<BundleRestartInstanceType, BundleRestartInstanceResponseType> {
    private BundleRestartInstanceCallback( BundleRestartInstanceType request ) {
      super( request );
    }
    
    @Override
    public void fire( BundleRestartInstanceResponseType reply ) {
      if ( !reply.get_return( ) ) {
          LOG.info( "Attempt to restart bundle instance " + this.getRequest( ).getInstanceId( ) + " has failed." );
      } else {
        EntityTransaction db = Entities.get( VmInstance.class );
        try {
          VmInstance vm = VmInstances.lookup( this.getRequest( ).getInstanceId( ) );
          vm.getRuntimeState( ).restartBundleTask( );
          EventRecord.here( CancelBundleCallback.class, EventType.BUNDLE_RESTART, this.getRequest( ).toSimpleString( ), vm.getRuntimeState( ).getBundleTask( ).getBundleId( ),
                            vm.getInstanceId( ) ).info( );
          db.commit( );
        } catch ( Exception ex ) {
          Logs.exhaust( ).error( ex, ex );
          db.rollback( );
        }
      }
    }
  }
  
  public static class BundleCallback extends MessageCallback<BundleInstanceType, BundleInstanceResponseType> {
    private BundleCallback( BundleInstanceType request ) {
      super( request );
    }
    
    @Override
    public void fire( BundleInstanceResponseType reply ) {
      EntityTransaction db = Entities.get( VmInstance.class );
      try {
        if ( !reply.get_return( ) ) {
          LOG.info( "Attempt to bundle instance " + this.getRequest( ).getInstanceId( ) + " has failed." );
        } else {
          VmInstance vm = VmInstances.lookup( this.getRequest( ).getInstanceId( ) );
          vm.getRuntimeState( ).submittedBundleTask( );
          EventRecord.here( BundleCallback.class, EventType.BUNDLE_STARTED, this.getRequest( ).toSimpleString( ), "" + vm.getRuntimeState( ),
                            vm.getInstanceId( ) ).info( );
        }
        db.commit( );
      } catch ( Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db.rollback( );
      }
    }
    
  }
  
  public static BundleTask transform( final VmBundleTask bundleTask ) {
    return VmBundleTask.asBundleTask( ).apply( bundleTask );
  }
  
  public static VmBundleTask create( VmInstance v, String bucket, String prefix, String policy ) throws AuthException {
    verifyPolicy( policy, bucket );
    // TODO: this was removed to get bundle-instance to work we still need to resolve the 
    // permissions issue see EUCA-3665
    //verifyBucket( bucket );
    // verifyPrefix( prefix );
    return VmBundleTask.create( v, bucket, prefix, policy );
  }
  
  private static void verifyPolicy( String policy, String bucketName ) {
    /**
     * GRZE:NOTE: why is there S3 specific stuff here? this is the ec2 implementation. policy check
     * must happen in walrus not here.
     **/
    // check if the policy is not user-generated one
    // "expiration": "2011-07-01T16:52:13","conditions": [{"bucket": "windowsbundle" },{"acl": "ec2-bundle-read" },["starts-with", "$key", "prefix"
    int idxOpenBracket = policy.indexOf( "{" );
    int idxClosingBracket = policy.lastIndexOf( "}" );
    if ( idxOpenBracket < 0 || idxClosingBracket < 0 || idxOpenBracket >= idxClosingBracket )
      throw new RuntimeException( "Custom policy is not acceptable for bundle instance" );
    
    String bucketAndAcl = policy.substring( idxOpenBracket, idxClosingBracket - idxOpenBracket );
    if ( !bucketAndAcl.contains( bucketName ) )
      throw new RuntimeException( "Custom policy is not acceptable for bundle instance" );
    if ( !bucketAndAcl.contains( "ec2-bundle-read" ) )
      throw new RuntimeException( "Custom policy is not acceptable for bundle instance" );
    
  }
  
  private static void verifyPrefix( String prefix ) {
    // check if the prefix name starts with "windows"
    if ( !prefix.startsWith( "windows" ) )
      /**
       * GRZE:NOTE: bundling is /not/ restricted to windows
       * only in general.
       * what is it doing here? should be set in the manifest by the NC.
       **/
      throw new RuntimeException( "Prefix name should start with 'windows'" );
  }
  
  /**
   * @param bucket
   * @throws AuthException 
   */
  private static void verifyBucket( final String bucketName ) throws AuthException {
    final Context ctx = Contexts.lookup( );
    final String accessKey = Accounts.getFirstActiveAccessKeyId( ctx.getUser( ) );
    CreateBucketType createBucket = new CreateBucketType( ) {
      {
        setAccessKeyID( accessKey );
        setBucket( bucketName );
      }
    }.regardingUserRequest( ctx.getRequest( ) );
    DeleteBucketType deleteBucket = new DeleteBucketType( ) {
      {
        setAccessKeyID( accessKey );
        setBucket( bucketName );
      }
    }.regardingUserRequest( ctx.getRequest( ) );
    ServiceConfiguration walrusConfig = Topology.lookup( Walrus.class );
    try {
      AsyncRequests.sendSync( walrusConfig, createBucket );
      AsyncRequests.sendSync( walrusConfig, deleteBucket );
    } catch ( Exception ex ) {
      throw new RuntimeException("Can't create the requested bucket", ex);
    }    
  }
}
