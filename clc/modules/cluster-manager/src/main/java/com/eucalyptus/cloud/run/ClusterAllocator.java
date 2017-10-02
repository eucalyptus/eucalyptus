/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.cloud.run;

import static com.eucalyptus.images.Images.findEbsRootOptionalSnapshot;
import static com.eucalyptus.util.Strings.regexReplace;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.EntityTransaction;

import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.VmInstanceLifecycleHelper;
import com.eucalyptus.cloud.VmInstanceToken;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.common.callback.ResourceStateCallback;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.BlockDeviceMappingItemType;
import com.eucalyptus.compute.common.backend.RunInstancesType;
import com.eucalyptus.compute.common.backend.StartInstancesType;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.tags.TagHelper;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import com.eucalyptus.cluster.common.msgs.DescribeResourcesResponseType;
import com.eucalyptus.cluster.common.msgs.DescribeResourcesType;

import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshot;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshots;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenResponseType;
import com.eucalyptus.blockstorage.msgs.GetVolumeTokenType;
import com.eucalyptus.blockstorage.msgs.StorageVolume;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.cluster.common.ResourceToken;
import com.eucalyptus.cluster.common.msgs.VmRunType;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.ResourceState;
import com.eucalyptus.cluster.callback.VmRunCallback;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.compute.common.internal.keys.SshKeyPair;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.tracking.MessageContexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.Request;
import com.eucalyptus.util.async.StatefulMessageSet;
import com.eucalyptus.compute.common.internal.vm.VmEphemeralAttachment;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.eucalyptus.cluster.common.msgs.VmKeyInfo;
import com.eucalyptus.cluster.common.msgs.VmRunResponseType;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecord;
import com.eucalyptus.cluster.common.msgs.VmTypeInfo;

public class ClusterAllocator implements Runnable {
  private static final long BYTES_PER_GB = ( 1024L * 1024L * 1024L );
  private static final Pattern SERVER_CERT_ACCOUNT_PATTERN = Pattern.compile( System.getProperty(
      "com.eucalyptus.cloud.run.serverCertAccountRegex",
      "(?s).*loadbalancer_owner_account\\s*=\\s*([0-9]{12}).*" ) );

  private static Logger     LOG          = Logger.getLogger( ClusterAllocator.class );
  
  public enum State {
    START,
    CREATE_VOLS,
    CREATE_IGROUPS,
    CREATE_NETWORK,
    CREATE_NETWORK_RULES,
    CREATE_VMS,
    UPDATE_RESOURCES,
    ATTACH_VOLS,
    ASSIGN_ADDRESSES,
    FINISHED,
    ROLLBACK,
  }
  
  private StatefulMessageSet<State> messages;
  private final Allocation          allocInfo;
  private Cluster                   cluster;
  
    
  enum SubmitAllocation implements Predicate<Allocation> {
    INSTANCE;
    
    @Override
    public boolean apply( final Allocation allocInfo ) {
      try {
        if ( EventRecord.isDebugEnabled( ClusterAllocator.class ) ) {
          EventRecord.here( ClusterAllocator.class, EventType.VM_PREPARE, LogUtil.dumpObject( allocInfo ) ).debug( );
        }
        final ServiceConfiguration config = Topology.lookup( ClusterController.class, allocInfo.getPartition( ) );
        final Callable<Boolean> runnable = new Callable<Boolean>( ) {
          @Override
          public Boolean call( ) {
            try {
              new ClusterAllocator( allocInfo, config ).run( );
            } catch ( final Exception ex ) {
              LOG.error( "Failed to prepare allocator for: " + allocInfo.getAllocationTokens( ), ex );
            }
            return Boolean.TRUE;
          }
        };
        BaseMessage baseReq = null; 
        for(final String instId : allocInfo.getInstanceIds()){
          baseReq = MessageContexts.lookupLast(instId, Sets.<Class>newHashSet(
              RunInstancesType.class,
              StartInstancesType.class
              ));
          if(baseReq!=null)
            break;
        }
        Threads.enqueue( config, SubmitAllocation.class, 32, runnable, baseReq == null ? null : baseReq.getCorrelationId() );
        return true;
      } catch ( final Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
  }
  
  public static Predicate<Allocation> get( ) {
    return SubmitAllocation.INSTANCE;
  }
  
  private ClusterAllocator( final Allocation allocInfo, final ServiceConfiguration clusterConfig ) {
    this.allocInfo = allocInfo;
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      this.cluster = Clusters.lookupAny( clusterConfig );
      this.messages = new StatefulMessageSet<State>( this.cluster, State.values( ) );
      this.setupVolumeMessages( );
      this.setupCredentialMessages( );
      this.updateResourceMessages( );
      db.commit( );
    } catch ( final Exception e ) {
      db.rollback( );
      cleanupOnFailure( allocInfo, e );
      return;
    }
    
    try {
      for ( final VmInstanceToken token : allocInfo.getAllocationTokens( ) ) {
        this.setupVmMessages( token );
      }
    } catch ( final Exception e ) {
      cleanupOnFailure( allocInfo, e );
    }
  }

  private void updateResourceMessages() {
    /**
     * NOTE: Here we need to do another resource refresh.
     * After an unordered instance type is run it is uncertain what the current resource
     * availability is.
     * This is the point at which the backing cluster would correctly respond w/ updated resource
     * counts.
     */
    Set<Cluster> clustersToUpdate = Sets.newHashSet();
    for ( final ResourceToken token : allocInfo.getAllocationTokens( ) ) {
      if ( token.isUnorderedType() ) {
        clustersToUpdate.add( token.getCluster() );
      }
    }
    for ( final Cluster cluster : clustersToUpdate ) {
      ResourceStateCallback cb = new ResourceStateCallback();
      cb.setSubject( cluster );
     
      Request<DescribeResourcesType,DescribeResourcesResponseType> request = AsyncRequests.newRequest( cb );
      this.messages.addRequest( State.UPDATE_RESOURCES, request );
    }
  }

  private void cleanupOnFailure( final Allocation allocInfo, final Exception e ) {
    LOG.error( e );
    Logs.extreme().error( e, e );
    this.allocInfo.abort( );
    for ( final VmInstanceToken token : allocInfo.getAllocationTokens( ) ) {
      try {
        final VmInstance vm = VmInstances.lookup( token.getInstanceId() );
        if ( VmState.STOPPED.equals( vm.getLastState( ) ) ) {
          VmInstances.stopped( vm );
        } else {
          VmInstances.terminated( vm );
        }
      } catch ( final Exception e1 ) {
        LOG.error( e1 );
        Logs.extreme( ).error( e1, e1 );
      }
    }
  }

  private void setupCredentialMessages( ) {
    try{
      final AccountIdentifiers accountIdentifiers =
          Accounts.lookupAccountIdentifiersById( allocInfo.getOwnerFullName( ).getAccountNumber( ) );
      if( !Accounts.isSystemAccount( accountIdentifiers.getAccountAlias() ) )
        return;
    }catch(final AuthException ex){
      return;
    }
    if (allocInfo.getUserData() == null || allocInfo.getUserData().length<=0)
      return;
    
    // determine if credential setup is requested
    final String userData = new String(allocInfo.getUserData());
    if(! VmInstances.VmSpecialUserData.apply(userData))
      return;
    
    String payload = null;
    int expirationDays = 180;
    try{
      final VmInstances.VmSpecialUserData specialData = new VmInstances.VmSpecialUserData(userData);
      if(! VmInstances.VmSpecialUserData.EUCAKEY_CRED_SETUP.equals(specialData.getKey() ))
        return;
      final String strExpDay = specialData.getExpirationDays();
      if (strExpDay != null )
        expirationDays = Integer.parseInt(strExpDay);
      payload = specialData.getPayload();
    }catch(final Exception ex) {
      LOG.error("Failed to parse VM user data", ex);
      return;
    }
    // update user data for instances in the reservation
    for(String s : this.allocInfo.getInstanceIds()) {
      try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
        final VmInstance instance = VmInstances.lookup( s );
        instance.setUserDataAsString(payload);
      } catch ( NoSuchElementException e ) {
        LOG.error("Can't find instance " + s + " to change its user data");
      }
    }

    // create rsa keypair
    try{
      final String endUserAccountNumber = Objects.firstNonNull(
          Strings.emptyToNull( Optional.of( payload ).transform( regexReplace( SERVER_CERT_ACCOUNT_PATTERN, "$1", "" ) ).orNull( ) ),
          allocInfo.getOwnerFullName( ).getAccountNumber( ) );
      final KeyPair kp = Certs.generateKeyPair();
      final String principal = String.format( "CN=%s, OU=Eucalyptus, O=Cloud, C=US", allocInfo.getInstanceId( 0 ) );
      final X509Certificate kpCert = Accounts.signCertificate( endUserAccountNumber, (RSAPublicKey) kp.getPublic(), principal, expirationDays );
      final String b64PubKey = B64.standard.encString( PEMFiles.getBytes( kpCert ) );
      
      // use NODECERT to encrypt the pk
      // generate symmetric key
      final MessageDigest digest = Digest.SHA256.get();
      final byte[] salt = new byte[32];
      Crypto.getSecureRandomSupplier().get().nextBytes(salt);
      digest.update( salt );
      final SecretKey symmKey = new SecretKeySpec( digest.digest(), "AES" );
      
      // encrypt the server pk
      Cipher cipher = Ciphers.AES_GCM.get();
      final byte[] iv = new byte[12];
      Crypto.getSecureRandomSupplier().get().nextBytes(iv);
      cipher.init( Cipher.ENCRYPT_MODE, symmKey, new IvParameterSpec( iv ), Crypto.getSecureRandomSupplier( ).get( ) );
      final byte[] cipherText = cipher.doFinal(Base64.encode(PEMFiles.getBytes(kp.getPrivate())));
      final String encPrivKey = new String(Base64.encode(Arrays.concatenate(iv, cipherText)));
            
      // with EUCA-8651, no signature needs to be delivered; left here for backward compatibility
      final String token = "NULL";
      // encrypt the token from EUARE
      cipher = Ciphers.AES_GCM.get();
      cipher.init( Cipher.ENCRYPT_MODE, symmKey, new IvParameterSpec( iv ), Crypto.getSecureRandomSupplier( ).get( ) );
      final byte[] byteToken = cipher.doFinal(token.getBytes());
      final String encToken = new String(Base64.encode(Arrays.concatenate(iv, byteToken)));
      
      // encrypt the symmetric key
      X509Certificate nodeCert = this.allocInfo.getPartition().getNodeCertificate();
      cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.ENCRYPT_MODE, nodeCert.getPublicKey(), Crypto.getSecureRandomSupplier( ).get( ));
      byte[] symmkey = cipher.doFinal(symmKey.getEncoded());
      final String encSymmKey = new String(Base64.encode(symmkey));
      
      X509Certificate euareCert = Accounts.getEuareCertificate( endUserAccountNumber );
      final String b64EuarePubkey = B64.standard.encString( PEMFiles.getBytes( euareCert ) );
    
      X509Certificate eucalyptusCert = SystemCredentials.lookup(Eucalyptus.class).getCertificate();
      final String b64EucalyptusPubkey = B64.standard.encString( PEMFiles.getBytes( eucalyptusCert ) );

      // EUARE's pubkey, VM's pubkey, token from EUARE(ENCRYPTED),
      // SYM_KEY(ENCRYPTED), VM_KEY(ENCRYPTED), EUCALYPTUS's pubkey
      // each field all in B64
      final String credential = String.format("%s\n%s\n%s\n%s\n%s\n%s",
          b64EuarePubkey,
          b64PubKey,
          encToken,
          encSymmKey, 
          encPrivKey,
          b64EucalyptusPubkey);
      this.allocInfo.setCredential(credential);
    }catch(final Exception ex){
      LOG.error("failed to setup instance credential", ex);
    }
  }

  // Modifying the logic to enable multiple block device mappings for boot from ebs. Fixes EUCA-3254 and implements EUCA-4786
  private void setupVolumeMessages( ) throws NoSuchElementException, MetadataException, ExecutionException {

    if (  this.allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo  ) {
      List<BlockDeviceMappingItemType> instanceDeviceMappings = new ArrayList<BlockDeviceMappingItemType>(this.allocInfo.getRequest().getBlockDeviceMapping());
      final ServiceConfiguration sc = Topology.lookup( Storage.class, this.cluster.getConfiguration( ).lookupPartition( ) );
      
      final BlockStorageImageInfo imgInfo = ( ( BlockStorageImageInfo ) this.allocInfo.getBootSet( ).getMachine( ) );   	                
      final String rootDevName = imgInfo.getRootDeviceName();
      Long volSizeBytes = imgInfo.getImageSizeBytes( );

      // Find out the root volume size so that device mappings that don't have a size or snapshot ID can use the root volume size
      for ( final BlockDeviceMappingItemType blockDevMapping : Iterables.filter( instanceDeviceMappings, findEbsRootOptionalSnapshot(rootDevName) ) ) {
        if ( blockDevMapping.getEbs( ).getVolumeSize( ) != null ) {
          volSizeBytes = BYTES_PER_GB * blockDevMapping.getEbs( ).getVolumeSize( );
        }
      } 

      int rootVolSizeInGb = ( int ) Math.ceil( ( ( double ) volSizeBytes ) / BYTES_PER_GB );

      for ( final VmInstanceToken token : this.allocInfo.getAllocationTokens( ) ) {
        final VmInstance vm = VmInstances.lookup( token.getInstanceId( ) );
        if ( !vm.getBootRecord( ).hasPersistentVolumes( ) ) { // No persistent volumes in the db
          if (!instanceDeviceMappings.isEmpty()) { // First time a bfebs instance starts up 
            for (final BlockDeviceMappingItemType mapping : instanceDeviceMappings) {
              if( Images.isEbsMapping( mapping ) ) {
                LOG.debug("About to prepare volume for instance " + vm.getDisplayName() + " to be mapped to " + mapping.getDeviceName() + " device");

                //spark - EUCA-7800: should explicitly set the volume size
                int volumeSize = mapping.getEbs().getVolumeSize()!=null? mapping.getEbs().getVolumeSize() : -1;
                if(volumeSize<=0){
                  if(mapping.getEbs().getSnapshotId() != null){
                    final Snapshot originalSnapshot = Snapshots.lookup(null, ResourceIdentifiers.tryNormalize().apply( mapping.getEbs().getSnapshotId() ) );
                    volumeSize = originalSnapshot.getVolumeSize();
                  }else
                    volumeSize = rootVolSizeInGb;
                }
                final AuthContextSupplier authContextSupplier;
                try {
                  authContextSupplier = this.allocInfo.getAuthContext( );
                } catch ( AuthException e ) {
                  throw new ExecutionException( e );
                }
                final UserFullName fullName = this.allocInfo.getOwnerFullName();
                final String authenticatedArn = this.allocInfo.getAuthenticatedArn();
                final String snapshotId = ResourceIdentifiers.tryNormalize().apply( mapping.getEbs().getSnapshotId() );
                final int volSize = volumeSize;
                final RunInstancesType request = this.allocInfo.getRequest();
                final Callable<Volume> createVolume = Contexts.callableWithContext( new Callable<Volume>( ) {
                    public Volume call( ) throws Exception {
                      final Function<Long, Volume> allocator = new Function<Long, Volume>( ) {
                        @Override
                        public Volume apply( Long size ) {
                          try {
                            return Volumes.createStorageVolume( sc, authenticatedArn, fullName, snapshotId, Ints.checkedCast( size ),
                                volume -> {
                                  final List<ResourceTag> volumeTags =
                                      TagHelper.tagsForResource( request.getTagSpecification( ), PolicySpec.EC2_RESOURCE_VOLUME );
                                  TagHelper.createOrUpdateTags( fullName, volume, volumeTags );
                                } );
                          } catch ( ExecutionException ex ) {
                            throw Exceptions.toUndeclared( ex );
                          }
                        }
                      };
                      return RestrictedTypes.allocateMeasurableResource(
                          authContextSupplier,
                          fullName,
                          RestrictedTypes.getIamActionByMessageType( request ),
                          (long) volSize,
                          allocator,
                          Volume.exampleResource( fullName, snapshotId, sc.getPartition( ), volSize ) );
                    }
                }, allocInfo.getContext( ) );
                final Volume volume; // allocate in separate transaction to ensure metadata matches back-end
                try {
                  volume = Threads.enqueue( Eucalyptus.class, ClusterAllocator.class, createVolume ).get( );
                } catch ( InterruptedException e ) {
                  throw Exceptions.toUndeclared( "Interrupted when creating volume from snapshot.", e );
                }

                final boolean isRootDevice = mapping.getDeviceName().equals(rootDevName);
                final boolean deleteOnTerminate = Objects.firstNonNull( mapping.getEbs().getDeleteOnTermination(), Boolean.FALSE );
                VmInstances.addPersistentVolume( vm, mapping.getDeviceName(), volume, isRootDevice, deleteOnTerminate );

                // Populate all volumes into resource token so they can be used for attach ops and vbr construction
                if( isRootDevice ) {
                  token.setRootVolume( volume );
                } else {
                  token.getEbsVolumes().put(mapping.getDeviceName(), volume);
                }
              } else if ( mapping.getVirtualName() != null ) {
                VmInstances.addEphemeralAttachment( vm, mapping.getDeviceName(), mapping.getVirtualName() );
                // Populate all ephemeral devices into resource token so they can used for vbr construction
                token.getEphemeralDisks().put(mapping.getDeviceName(), mapping.getVirtualName());
              }
            }
          } else { // Stopped instance is started with no attached volumes
            LOG.error("Volume attachment for root device not found. Attach an EBS volume to root device of " + vm.getInstanceId() + " and retry");
            throw new MetadataException("Volume attachment for root device not found. Attach an EBS volume to root device of " + vm.getInstanceId()
                + " and retry");
          }
        } else { // This block is hit when starting a stopped bfebs instance
          // Although volume attachment records exist and the volumes are marked attached, all volumes are in detached state when the instance is stopped. 
          // Go through all volume attachments and populate them into the resource token so they can be used for attach ops and vbr construction
          boolean foundRoot = false;
          for (VmVolumeAttachment attachment : vm.getBootRecord( ).getPersistentVolumes( )) {
        	final Volume volume = Volumes.lookup( null, attachment.getVolumeId( ) );
            if (attachment.getIsRootDevice() || attachment.getDevice().equals(rootDevName) ) {
              token.setRootVolume( volume );
              foundRoot = true;
            } else {
              token.getEbsVolumes().put(attachment.getDevice(), volume);
            }
          }
          
          // Root volume may have been detached. In that case throw an error and exit
          if ( !foundRoot ) {
            LOG.error("Volume attachment for root device not found. Attach an EBS volume to root device of " + vm.getInstanceId() + " and retry");
            throw new MetadataException("Volume attachment for root device not found. Attach an EBS volume to root device of " + vm.getInstanceId()
                + " and retry");
          }
          
          // Fix for EUCA-6947. Go through all transient attachments (volumes attached to instance at run time) and add them to resource token so they
          // can be included in the VBR sent to CC/NC
          for (VmVolumeAttachment attachment : vm.getTransientVolumeState().getAttachments()) {
            final Volume volume = Volumes.lookup(null, attachment.getVolumeId());
            token.getEbsVolumes().put(attachment.getDevice(), volume);
          }
          
          // Go through all ephemeral attachment records and populate them into resource token so they can used for vbr construction
          for (VmEphemeralAttachment attachment : vm.getBootRecord( ).getEphemeralStorage()) {
            token.getEphemeralDisks().put(attachment.getDevice(), attachment.getEphemeralId());
          }
        }
      }
    }
  }
  
  private void setupVmMessages( final VmInstanceToken token ) throws Exception {
    final VmTypeInfo vmInfo = this.allocInfo.getVmTypeInfo( this.allocInfo.getPartition( ), token.getAllocationInfo().getReservationId() );
    allocInfo.setRootDirective();
    try {
      final VmTypeInfo childVmInfo = this.makeVmTypeInfo( vmInfo, token );
      final VmRunCallback callback = this.makeRunCallback( token, childVmInfo );
      final Request<VmRunType, VmRunResponseType> req = AsyncRequests.newRequest( callback );
      
      this.messages.addRequest( State.CREATE_VMS, req );
      this.messages.addCleanup( new Runnable( ) {
        @Override
        public void run( ) {
          if ( token.isPending( ) ) try {
            token.release( );
          } catch ( final ResourceState.NoSuchTokenException e ) {
            Logs.extreme( ).error( e, e );
          }
        }
      } );
      LOG.debug( "Queued RunInstances: " + token );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw ex;
    }
  }
  
  // Modifying the logic to enable multiple block device mappings for boot from ebs. Fixes EUCA-3254 and implements EUCA-4786
  // Using resource token to construct vbr record rather than volume attachments from the database as there might be race condition
  // where the vm instance record may not have been updated with the volume attachments. EUCA-5670
  private VmTypeInfo makeVmTypeInfo( final VmTypeInfo vmInfo, final VmInstanceToken token ) throws Exception {
    VmTypeInfo childVmInfo = vmInfo.child( );
    
    if ( this.allocInfo.getBootSet( ).getMachine( ) instanceof BlockStorageImageInfo ) {        
    	String instanceId = token.getInstanceId();
    	final VmInstance vm = VmInstances.lookup(instanceId);
        Map<String, String> volumeAttachmentTokenMap = Maps.newHashMap();
    	
    	// Deal with the root volume first
    	VirtualBootRecord rootVbr = childVmInfo.lookupRoot();
    	Volume rootVolume = token.getRootVolume();
    	String volumeId = rootVolume.getDisplayName( );
    	String volumeToken = null;
    	
    	// Wait for root volume
    	LOG.debug("Wait for root ebs volume " + rootVolume.getDisplayName() +  " to become available");
    	final ServiceConfiguration scConfig = waitForVolume(rootVolume);
    	
    	// Attach root volume
    	try {
    		LOG.debug("About to get attachment token for volume " + rootVolume.getDisplayName() + " to instance " + instanceId);    		
    		GetVolumeTokenResponseType scGetTokenResponse;
    		try {
    			GetVolumeTokenType req = new GetVolumeTokenType(volumeId);
    			scGetTokenResponse = AsyncRequests.sendSync(scConfig, req);
    		} catch ( Exception e ) {
    			LOG.debug( e, e );
    			throw new EucalyptusCloudException( e.getMessage( ), e );
    		}
    		
    		LOG.debug("Got volume token response from SC for volume " + rootVolume.getDisplayName() + " and instance " + instanceId + "\n" + scGetTokenResponse);
    		volumeToken = scGetTokenResponse.getToken();                
    		if ( volumeToken == null ) {
    			throw new EucalyptusCloudException( "Failed to get remote device string for " + volumeId + " while running instance " + token.getInstanceId( ) );
    		} else {
    			//Do formatting here since formatting is for messaging only.
    			volumeToken = StorageProperties.formatVolumeAttachmentTokenForTransfer(volumeToken, volumeId);
    		}
    		rootVbr.setResourceLocation(volumeToken);
    		rootVbr.setSize(rootVolume.getSize() * BYTES_PER_GB);
    		volumeAttachmentTokenMap.put(volumeId, volumeToken);
    	} catch (final Exception ex) {
    		LOG.error(ex);
    		Logs.extreme().error(ex, ex);
    		throw ex;
    	}
    	
    	// Deal with the remaining ebs volumes
    	for (Entry<String, Volume> mapping : token.getEbsVolumes().entrySet()) {
    		Volume volume = mapping.getValue();
    		if (volume.getSize() <= 0) {
    			volume = Volumes.lookup(this.allocInfo.getOwnerFullName(), mapping.getValue().getDisplayName());	
    		}
    		volumeId = volume.getDisplayName();  
    		
    		LOG.debug("Wait for volume " + volumeId +  " to become available");
    		final ServiceConfiguration scConfigLocal = waitForVolume(volume);

    		try {
    			LOG.debug("About to get attachment token for volume " + volume.getDisplayName() + " to instance " + instanceId);    		
    			GetVolumeTokenResponseType scGetTokenResponse;
    			try {
    				GetVolumeTokenType req = new GetVolumeTokenType(volumeId);
    				scGetTokenResponse = AsyncRequests.sendSync(scConfigLocal, req);
    			} catch ( Exception e ) {
    				LOG.debug( e, e );
    				throw new EucalyptusCloudException( e.getMessage( ), e );
    			}
    			
    			LOG.debug("Got volume token response from SC for volume " + volume.getDisplayName() + " and instance " + instanceId + "\n" + scGetTokenResponse);
    			volumeToken = scGetTokenResponse.getToken();
    			if ( volumeToken == null ) {
    				throw new EucalyptusCloudException( "Failed to get remote device string for " + volumeId + " while running instance " + token.getInstanceId( ) );
    			} else {
    				//Do formatting here since formatting is for messaging only.
    				volumeToken = StorageProperties.formatVolumeAttachmentTokenForTransfer(volumeToken, volumeId);
    				VirtualBootRecord vbr = new VirtualBootRecord(volumeId, volumeToken, "ebs", mapping.getKey(), (volume.getSize() * BYTES_PER_GB), "none");
    				childVmInfo.getVirtualBootRecord().add(vbr);
    				volumeAttachmentTokenMap.put(volumeId, volumeToken);
    			}
    		} catch (final Exception ex) {
    			LOG.error(ex);
    			Logs.extreme().error(ex, ex);
    			throw ex;
    		}
    	}
    	
    	// update volume attachment tokens in database
        if (!volumeAttachmentTokenMap.isEmpty()) {
          VmInstances.updateAttachmentToken( vm, volumeAttachmentTokenMap );
        }
    	
    	// FIXME: multiple ephemerals will result in wrong disk sizes
    	for( String deviceName : token.getEphemeralDisks().keySet()  ) {
    		childVmInfo.setEphemeral( 0, deviceName, (this.allocInfo.getVmType().getDisk( ) * BYTES_PER_GB), "none" );
    	}
    	
    	LOG.debug("Instance information: " + childVmInfo.dump());
    }
    return childVmInfo;
  }
  
  public ServiceConfiguration waitForVolume( final Volume vol ) throws Exception {
    final ServiceConfiguration scConfig = Topology.lookup( Storage.class, Partitions.lookupByName( vol.getPartition( ) ) );
    long startTime = System.currentTimeMillis( );
    int numDescVolError = 0;
    while ( ( System.currentTimeMillis( ) - startTime ) < VmInstances.EBS_VOLUME_CREATION_TIMEOUT * 60 * 1000L ) {
      try {
        DescribeStorageVolumesResponseType volState = null;
        try {
          final DescribeStorageVolumesType describeMsg = new DescribeStorageVolumesType( Lists.newArrayList( vol.getDisplayName( ) ) );
          volState = AsyncRequests.sendSync( scConfig, describeMsg );
        } catch ( final Exception e ) {
          if ( numDescVolError++ < 5 ) {
            try {
              TimeUnit.SECONDS.sleep( 5 );
            } catch ( final InterruptedException ex ) {
              Thread.currentThread( ).interrupt( );
            }
            continue;
          } else {
            throw e;
          }
        }
        StorageVolume storageVolume = volState.getVolumeSet( ).get( 0 );
        LOG.debug( "Got storage volume info: " + storageVolume );
        if ( "available".equals( storageVolume.getStatus( ) ) ) {
          return scConfig;
        } else if ( "failed".equals( storageVolume.getStatus( ) ) ) {
          throw new EucalyptusCloudException( "volume creation failed" );
        } else {
          TimeUnit.SECONDS.sleep( 5 );
        }
      } catch ( final InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
      } catch ( final Exception ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    throw new EucalyptusCloudException( "volume " + vol.getDisplayName( ) + " was not created in time" );
  }
  
  private VmRunCallback makeRunCallback( final VmInstanceToken childToken, final VmTypeInfo vmInfo ) {
    final SshKeyPair keyPair = this.allocInfo.getSshKeyPair( );
    final VmKeyInfo vmKeyInfo = new VmKeyInfo( keyPair.getName( ), keyPair.getPublicKey( ), keyPair.getFingerPrint( ) );
    final String platform = this.allocInfo.getBootSet( ).getMachine( ).getPlatform( ).name( ) != null
                                                                                                     ? this.allocInfo.getBootSet( ).getMachine( ).getPlatform( ).name( )
                                                                                                     : "linux"; // ASAP:FIXME:GRZE
    
//TODO:GRZE:FINISH THIS.    Date date = Contexts.lookup( ).getContracts( ).get( Contract.Type.EXPIRATION ); 
    final VmRunType.Builder builder = VmRunType.builder( );
    VmInstanceLifecycleHelper.get( ).prepareVmRunType( childToken, builder );
    final VmRunType run = builder
                                   .instanceId( childToken.getInstanceId( ) )
                                   .naturalId( childToken.getInstanceUuid( ) )
                                   .keyInfo( vmKeyInfo )
                                   .launchIndex( childToken.getLaunchIndex( ) )
                                   .networkNames( this.allocInfo.getNetworkGroups( ) )
                                   .networkIds( this.allocInfo.getNetworkGroups( ) )
                                   .platform( platform )
                                   .reservationId( childToken.getAllocationInfo( ).getReservationId( ) )
                                   .userData( this.allocInfo.getRequest( ).getUserData( ) )
                                   .credential( this.allocInfo.getCredential( ) )
                                   .vmTypeInfo( vmInfo )
                                   .owner( this.allocInfo.getOwnerFullName( ) )
                                   .rootDirective( this.allocInfo.getRootDirective() )
                                   .create( );
    if ( LOG.isDebugEnabled( ) ) {
      LOG.debug( "Run instance request: " + run );
    }
    return new VmRunCallback( run, childToken );
  }
  
  @Override
  public void run( ) {
    this.messages.run( );
  }
  
}
