/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.vm;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.cluster.common.msgs.ClusterBundleInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleRestartInstanceResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterBundleRestartInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterCancelBundleTaskResponseType;
import com.eucalyptus.cluster.common.msgs.ClusterCancelBundleTaskType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.BundleTask;
import com.eucalyptus.compute.common.internal.vm.VmBundleTask;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmRuntimeState;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.MessageCallback;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;


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
  
  public static MessageCallback createCallback( ClusterBundleInstanceType request ) throws AuthException, IllegalContextAccessException, ServiceStateException {
    final String objectStorageUrl = ServiceUris.remote( Topology.lookup( ObjectStorage.class ) ).toASCIIString( );
    request.setUrl( objectStorageUrl );
    return new BundleCallback( request );
  }
  
  public static MessageCallback cancelCallback( ClusterCancelBundleTaskType request ) {
    return new CancelBundleCallback( request );
  }

  public static void updateBundleTaskState( final VmInstance vm, String state ) {
    VmBundleTask.BundleState next = VmBundleTask.BundleState.mapper.apply( state );
    updateBundleTaskState( vm, next, 0.0d );
  }

  public static void bundleRestartInstance( VmBundleTask bundleTask ) {
    VmBundleTask.BundleState state = bundleTask.getState( );
    if ( VmBundleTask.BundleState.complete.equals( state ) || VmBundleTask.BundleState.failed.equals( state ) || VmBundleTask.BundleState.cancelled.equals( state ) ) {
      final ClusterBundleRestartInstanceType request = new ClusterBundleRestartInstanceType( );
      try {
        LOG.info( EventRecord.here( BundleCallback.class, EventType.BUNDLE_RESTART, bundleTask.getVmInstance().getOwner().getUserName(),
            bundleTask.getBundleId(),
            bundleTask.getVmInstance().getInstanceId() ) );

        ServiceConfiguration ccConfig = Topology.lookup( ClusterController.class, bundleTask.getVmInstance( ).lookupPartition() );

        request.setInstanceId( bundleTask.getVmInstance( ).getInstanceId() );
        AsyncRequests.newRequest( bundleRestartInstanceCallback( request ) ).dispatch( ccConfig );
      } catch ( final Exception e ) {
        Logs.extreme( ).trace( "Failed to find bundle task: " + bundleTask.getBundleId( ) );
      }
    }
  }

  private static void setBundleTaskState( final VmBundleTask task, final VmBundleTask.BundleState state ) {
    final VmBundleTask.BundleState previousState = task.getState( );
    if ( previousState != null && previousState != state ) {
      putPreviousTask( task );
    }
    task.setState( state );
  }

  public static void updateBundleTaskState( VmInstance vm, VmBundleTask.BundleState state, Double progress ) {
    if ( vm.getRuntimeState( ).getBundleTask() != null ) {
      final VmBundleTask.BundleState current = vm.getRuntimeState( ).getBundleTask().getState( );
      VmBundleTask currentTask = vm.getRuntimeState( ).getBundleTask();
      currentTask.setProgress((int) Math.round(progress * 100D));
      if ( VmBundleTask.BundleState.complete.equals( state ) && !VmBundleTask.BundleState.complete.equals( current ) && !VmBundleTask.BundleState.none.equals( current ) ) {
        setBundleTaskState( currentTask, state );
        // set progress to 100% if complete is reached
        currentTask.setProgress( 100 );
        bundleRestartInstance( currentTask );
      } else if ( VmBundleTask.BundleState.failed.equals( state ) && !VmBundleTask.BundleState.failed.equals( current ) && !VmBundleTask.BundleState.none.equals( current ) ) {
        try{
          deleteBucketContent(
              vm.getOwnerAccountNumber( ),
              currentTask.getBucket( ),
              currentTask.getPrefix( ),
              true );
        }catch(final Exception ex){
          LOG.error("After bundle failure, failed to delete the bucket", ex);
        }
        setBundleTaskState( currentTask, state );
        bundleRestartInstance( currentTask );
      } else if ( VmBundleTask.BundleState.cancelled.equals( state ) && !VmBundleTask.BundleState.cancelled.equals( current ) && !VmBundleTask.BundleState.none.equals( current ) ) {
        try{
          deleteBucketContent(
              vm.getOwnerAccountNumber(),
              vm.getRuntimeState( ).getBundleTask().getBucket( ),
              vm.getRuntimeState( ).getBundleTask().getPrefix( ),
              true );
        }catch(final Exception ex){
          LOG.error("After bundle cancellation, failed to delete the bucket", ex);
        }
        setBundleTaskState( currentTask, state );
        bundleRestartInstance( currentTask );
      } else if ( VmBundleTask.BundleState.canceling.equals( state ) || VmBundleTask.BundleState.canceling.equals( current ) ) {
        //
      } else if ( VmBundleTask.BundleState.pending.equals( current ) && !VmBundleTask.BundleState.none.equals( state ) ) {
        setBundleTaskState( currentTask, state );
        currentTask.setUpdateTime( new Date( ) );
        EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_TRANSITION, vm.getOwner( ).toString( ), "" + vm.getRuntimeState( ).getBundleTask() ).info( );
      } else if ( VmBundleTask.BundleState.storing.equals( state ) ) {
        setBundleTaskState( currentTask, state );
        currentTask.setUpdateTime( new Date( ) );
        EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_TRANSITION, vm.getOwner( ).toString( ), "" + vm.getRuntimeState( ).getBundleTask() ).info( );
      }
    } else {
      putPreviousTask( vm.getRuntimeState( ).getBundleTask( ) );
      vm.getRuntimeState( ).setBundleTask( new VmBundleTask( vm, state.name(), new Date(), new Date(), 0, "unknown", "unknown", "unknown", "unknown" ) );
      Logs.extreme( ).trace( "Unhandle bundle task state update: " + state );
    }
  }

  public static Boolean cancelBundleTask( VmRuntimeState state ) {
    if ( state.getBundleTask() != null ) {
      setBundleTaskState( state.getBundleTask(), VmBundleTask.BundleState.canceling );
      EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_CANCELING, state.getVmInstance ().getOwner().toString( ), state.getBundleTask().getBundleId( ),
          state.getVmInstance().getInstanceId( ),
          "" + state.getBundleTask().getState( ) ).info( );
      return true;
    } else {
      return false;
    }
  }

  public static Boolean restartBundleTask( VmRuntimeState state ) {
    if ( state.getBundleTask() != null ) {
      setBundleTaskState( state.getBundleTask(), VmBundleTask.BundleState.none );
      EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_RESTART, state.getVmInstance().getOwner().toString( ), state.getBundleTask().getBundleId( ),
          state.getVmInstance().getInstanceId( ),
          "" + state.getBundleTask().getState( ) ).info( );
      return true;
    }
    return false;
  }

  public static Boolean submittedBundleTask( VmRuntimeState state ) {
    if ( state.getBundleTask() != null ) {
      if ( VmBundleTask.BundleState.cancelled.equals( state.getBundleTaskState() ) ) {
        EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_CANCELLED, state.getVmInstance().getOwner().toString( ), state.getBundleTask().getBundleId( ),
            state.getVmInstance().getInstanceId( ),
            "" + state.getBundleTask().getState( ) ).info( );
        resetBundleTask( state );
        return true;
      } else if ( state.getBundleTask().getState( ).ordinal( ) >= VmBundleTask.BundleState.storing.ordinal( ) ) {
        setBundleTaskState( state.getBundleTask(), VmBundleTask.BundleState.storing );
        EventRecord.here( VmRuntimeState.class, EventType.BUNDLE_STARTING,
            state.getVmInstance().getOwner( ).toString( ),
            state.getBundleTask( ).getBundleId(),
            state.getVmInstance( ).getInstanceId( ),
            "" + state.getBundleTask( ).getState( ) ).info();
        return true;
      }
    }
    return false;
  }

  public static Boolean startBundleTask( final VmBundleTask task ) {
    final VmRuntimeState state = task.getVmInstance( ).getRuntimeState( );
    if ( !state.isBundling( ) ) {
      putPreviousTask( state.getBundleTask( ) );
      state.setBundleTask( task );
      return true;
    } else {
      if ( ( state.getBundleTask() != null )
          && ( VmBundleTask.BundleState.failed.equals( task.getState( ) ) || VmBundleTask.BundleState.canceling.equals( task.getState( ) ) || VmBundleTask.BundleState.cancelled.equals( task.getState( ) ) ) ) {
        resetBundleTask( state );
        state.setBundleTask( task );
        return true;
      } else {
        return false;
      }
    }
  }

  private static VmBundleTask resetBundleTask( final VmRuntimeState state ) {
    final VmBundleTask oldTask = state.getBundleTask( );
    putPreviousTask(oldTask);
    state.setBundleTask( null );
    return oldTask;
  }

  public static class CancelBundleCallback extends MessageCallback<ClusterCancelBundleTaskType, ClusterCancelBundleTaskResponseType> {
    private CancelBundleCallback( ClusterCancelBundleTaskType request ) {
      super( request );
    }
    
    @Override
    public void fire( ClusterCancelBundleTaskResponseType reply ) {
      if ( !reply.get_return( ) ) {
        LOG.info( "Attempt to CancelBundleTask for instance " + this.getRequest( ).getBundleId( ) + " has failed." );
      } else {
        EntityTransaction db = Entities.get( VmInstance.class );
        try {
          VmInstance vm = VmInstances.lookupByBundleId( this.getRequest( ).getBundleId( ) );
          cancelBundleTask( vm.getRuntimeState() );
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
  
  public static MessageCallback bundleRestartInstanceCallback( ClusterBundleRestartInstanceType request ) {
    return new BundleRestartInstanceCallback( request );
  }
  
  public static class BundleRestartInstanceCallback extends MessageCallback<ClusterBundleRestartInstanceType, ClusterBundleRestartInstanceResponseType> {
    private BundleRestartInstanceCallback( ClusterBundleRestartInstanceType request ) {
      super( request );
    }
    
    @Override
    public void fire( ClusterBundleRestartInstanceResponseType reply ) {
      if ( !reply.get_return( ) ) {
          LOG.info( "Attempt to restart bundle instance " + this.getRequest( ).getInstanceId( ) + " has failed." );
      } else {
        EntityTransaction db = Entities.get( VmInstance.class );
        try {
          VmInstance vm = VmInstances.lookup( this.getRequest( ).getInstanceId( ) );
          restartBundleTask( vm.getRuntimeState() );
          EventRecord.here( BundleRestartInstanceCallback.class, EventType.BUNDLE_RESTART, this.getRequest( ).toSimpleString( ), vm.getRuntimeState( ).getBundleTask( ).getBundleId( ),
                            vm.getInstanceId( ) ).info( );
          db.commit( );
        } catch ( Exception ex ) {
          Logs.exhaust( ).error( ex, ex );
          db.rollback( );
        }
      }
    }
  }
  
  public static class BundleCallback extends MessageCallback<ClusterBundleInstanceType, ClusterBundleInstanceResponseType> {
    private BundleCallback( ClusterBundleInstanceType request ) {
      super( request );
    }
    
    @Override
    public void fire( ClusterBundleInstanceResponseType reply ) {
      EntityTransaction db = Entities.get( VmInstance.class );
      try {
        if ( !reply.get_return( ) ) {
          LOG.info( "Attempt to bundle instance " + this.getRequest( ).getInstanceId( ) + " has failed." );
        } else {
          VmInstance vm = VmInstances.lookup( this.getRequest( ).getInstanceId( ) );
          submittedBundleTask( vm.getRuntimeState() );
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

  public static Function<VmBundleTask, BundleTask> asBundleTask( ) {
    return new Function<VmBundleTask, BundleTask>( ) {

      @Override
      public BundleTask apply( final VmBundleTask input ) {
        return new BundleTask( input.getInstanceId( ),
            input.getBundleId( ),
            input.getState( ).name( ),
            input.getStartTime( ),
            input.getUpdateTime( ),
            "" + input.getProgress( ),
            input.getBucket( ),
            input.getPrefix( ),
            input.getErrorMessage( ),
            input.getErrorCode( ) );
      }
    };
  }



  public static BundleTask transform( final VmBundleTask bundleTask ) {
    return asBundleTask( ).apply( bundleTask );
  }
  
  public static VmBundleTask create( VmInstance v, String bucket, String prefix, String policy ) throws AuthException {
    verifyPolicy( policy, bucket );
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
  
  static void checkAndCreateBucket( final User user, final String bucketName, final String prefix ) throws ComputeException {
    try ( final EucaS3Client s3c =
              EucaS3ClientFactory.getEucaS3Client( SecurityTokenAWSCredentialsProvider.forUserOrRole( user ) ) ) {
      boolean foundBucket = false;
      final List<Bucket> buckets = s3c.listBuckets( );
      for( final Bucket bucket : buckets ) {
        foundBucket = bucketName.equals( bucket.getName( ) );
        if ( foundBucket ) {
          if ( !Strings.isNullOrEmpty( prefix ) ) {
            final ObjectListing objects = s3c.listObjects( bucketName, Strings.emptyToNull( prefix ) );
            if ( !objects.getObjectSummaries( ).isEmpty( ) ) {
              throw new ClientComputeException(
                  "InvalidParameterValue",
                  "Bucket prefix in use " + bucketName + "/" + prefix );
            }
          }
          break;
        }
      }
      if ( !foundBucket ) {
        s3c.createBucket( bucketName );
      }
    } catch( final Exception ex ) {
      Exceptions.findAndRethrow( ex, ComputeException.class );
      if ( ex instanceof AmazonS3Exception && "BucketAlreadyExists".equals(((AmazonS3Exception)ex).getErrorCode( ) ) ) {
        throw new ClientComputeException(
            "InvalidParameterValue",
            "Bucket already exists " + bucketName );
      }
      LOG.debug( "Unable to create bucket " + bucketName, ex);
      throw new ComputeException( "InternalError", "Unable to create bucket " + bucketName );
    }
  }

  static void deleteBucketContent( final String accountId,
                                   final String bucketName,
                                   final String prefix,
                                   final boolean deleteEmptyBucket ) throws ComputeException {
    try ( final EucaS3Client s3c =
              EucaS3ClientFactory.getEucaS3Client( new SecurityTokenAWSCredentialsProvider( AccountFullName.getInstance( accountId ) ) ) ) {
      final List<Bucket> buckets = s3c.listBuckets( );
      boolean bucketFound = false;
      for(final Bucket bucket : buckets){
        if(bucketName.equals(bucket.getName())){
          bucketFound=true;
          break;
        }
      }
      if( bucketFound ){
        final ObjectListing objects = s3c.listObjects( bucketName, Strings.emptyToNull( prefix ) );
        for( final S3ObjectSummary object : objects.getObjectSummaries( ) ){
          s3c.deleteObject( bucketName, object.getKey( ) );
        }
        if ( deleteEmptyBucket ) {
          final VersionListing versions = s3c.listVersions( bucketName, null );
          if ( versions.getVersionSummaries( ).isEmpty( ) ) {
            s3c.deleteBucket( bucketName );
          }
        }
      }
    } catch( final Exception ex ){
      LOG.debug( "Error deleting bucket ("+bucketName+") " +
          ( Strings.isNullOrEmpty( prefix ) ? "" : "prefix ("+prefix+") " ) + "for account " + accountId, ex );
      throw new ComputeException("InternalError", "Unable to delete the bucket");
    }
  }
}
