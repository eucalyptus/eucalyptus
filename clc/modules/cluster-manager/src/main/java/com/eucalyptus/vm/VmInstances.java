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
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.vm;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.blockstorage.State;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.cloud.CloudMetadata.VmInstanceMetadata;
import com.eucalyptus.cluster.callback.TerminateCallback;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.images.BlockStorageImageInfo;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.eucalyptus.util.RestrictedTypes.Resolver;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstance.Transitions;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance.VmStateSet;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.DeleteStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.DetachStorageVolumeType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;

@ConfigurableClass( root = "cloud.vmstate",
                    description = "Parameters controlling the lifecycle of virtual machines." )
public class VmInstances {
  public static class TerminatedInstanceException extends NoSuchElementException {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    TerminatedInstanceException( final String s ) {
      super( s );
    }
    
  }
  
  public enum Timeout implements Predicate<VmInstance> {
    EXPIRED( VmState.RUNNING ) {
      @Override
      public Integer getMinutes( ) {
        return 0;
      }
      
      @Override
      public boolean apply( VmInstance arg0 ) {
        return VmState.RUNNING.apply( arg0 ) && ( System.currentTimeMillis( ) > arg0.getExpiration( ).getTime( ) );
      }
    },
    UNREPORTED( VmState.PENDING, VmState.RUNNING ) {
      @Override
      public Integer getMinutes( ) {
        return INSTANCE_TIMEOUT;
      }
    },
    SHUTTING_DOWN( VmState.SHUTTING_DOWN ) {
      @Override
      public Integer getMinutes( ) {
        return SHUT_DOWN_TIME;
      }
    },
    STOPPING( VmState.STOPPING ) {
      @Override
      public Integer getMinutes( ) {
        return STOPPING_TIME;
      }
    },
    TERMINATED( VmState.TERMINATED ) {
      @Override
      public Integer getMinutes( ) {
        return TERMINATED_TIME;
      }
    };
    private final List<VmState> states;
    
    private Timeout( final VmState... states ) {
      this.states = Arrays.asList( states );
    }
    
    public abstract Integer getMinutes( );
    
    public Integer getSeconds( ) {
      return this.getMinutes( ) * 60;
    }
    
    public Long getMilliseconds( ) {
      return this.getSeconds( ) * 1000l;
    }
    
    @Override
    public boolean apply( final VmInstance arg0 ) {
      return this.inState( arg0.getState( ) ) && ( arg0.getSplitTime( ) > this.getMilliseconds( ) );
    }
    
    protected boolean inState( final VmState state ) {
      return this.states.contains( state );
    }
    
  }
  
  @ConfigurableField( description = "Number of times to retry transactions in the face of potential concurrent update conflicts.",
                      initial = "10" )
  public static final int TX_RETRIES                    = 10;
  @ConfigurableField( description = "Amount of time (in minutes) before a previously running instance which is not reported will be marked as terminated.",
                      initial = "60" )
  public static Integer   INSTANCE_TIMEOUT              = 60;
  @ConfigurableField( description = "Amount of time (in minutes) before a VM which is not reported by a cluster will be marked as terminated.",
                      initial = "10" )
  public static Integer   SHUT_DOWN_TIME                = 10;
  @ConfigurableField( description = "Amount of time (in minutes) before a stopping VM which is not reported by a cluster will be marked as terminated.",
                      initial = "10" )
  public static Integer   STOPPING_TIME                 = 10;
  @ConfigurableField( description = "Amount of time (in minutes) that a terminated VM will continue to be reported.",
                      initial = "60" )
  public static Integer   TERMINATED_TIME               = 60;
  @ConfigurableField( description = "Maximum amount of time (in seconds) that the network topology service takes to propagate state changes.",
                      initial = "" + 60 * 60 * 1000 )
  public static Long      NETWORK_METADATA_REFRESH_TIME = 15l;
  @ConfigurableField( description = "Prefix to use for instance MAC addresses.",
                      initial = "d0:0d" )
  public static String    MAC_PREFIX                    = "d0:0d";
  @ConfigurableField( description = "Subdomain to use for instance DNS.",
                      initial = ".eucalyptus",
                      changeListener = SubdomainListener.class )
  public static String    INSTANCE_SUBDOMAIN            = ".eucalyptus";
  @ConfigurableField( description = "Period (in seconds) between state updates for actively changing state.",
                      initial = "3" )
  public static Long      VOLATILE_STATE_INTERVAL_SEC   = Long.MAX_VALUE;
  @ConfigurableField( description = "Timeout (in seconds) before a requested instance terminate will be repeated.",
                      initial = "60" )
  public static Long      VOLATILE_STATE_TIMEOUT_SEC    = 60l;
  @ConfigurableField( description = "Maximum number of threads the system will use to service blocking state changes.",
                      initial = "16" )
  public static Integer   MAX_STATE_THREADS             = 16;
  @ConfigurableField( description = "Amount of time (in minutes) before a EBS volume backing the instance is created",
                      initial = "30" )
  public static Integer   EBS_VOLUME_CREATION_TIMEOUT   = 30;
  
  public static class SubdomainListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      
      if ( !newValue.toString( ).startsWith( "." ) || newValue.toString( ).endsWith( "." ) )
        throw new ConfigurablePropertyException( "Subdomain must begin and cannot end with a '.' -- e.g., '." + newValue.toString( ).replaceAll( "\\.$", "" )
                                                 + "' is correct." + t.getFieldName( ) );
      
    }
  }
  
  static ConcurrentMap<String, VmInstance>               terminateCache         = new ConcurrentHashMap<String, VmInstance>( );
  static ConcurrentMap<String, RunningInstancesItemType> terminateDescribeCache = new ConcurrentHashMap<String, RunningInstancesItemType>( );
  
  private static Logger                                  LOG                    = Logger.getLogger( VmInstances.class );
  
  @QuantityMetricFunction( VmInstanceMetadata.class )
  public enum CountVmInstances implements Function<OwnerFullName, Long> {
    INSTANCE;
    
    @Override
    public Long apply( final OwnerFullName input ) {
      final EntityTransaction db = Entities.get( VmInstance.class );
      final int i;
      try {
        i = Entities.createCriteria( VmInstance.class )
                    .add( Example.create( VmInstance.named( input, null ) ) )
                    .setReadOnly( true )
                    .setCacheable( false )
                    .list( )
                    .size( );
      } finally {
        db.rollback( );
      }
      return ( long ) i;
    }
  }
  
  public static String getId( final Long rsvId, final int launchIndex ) {
    String vmId = null;
    do {
      vmId = Crypto.generateId( Long.toString( rsvId + launchIndex ), "i" );
    } while ( VmInstances.contains( vmId ) );
    return vmId;
  }
  
  public static VmInstance lookupByPrivateIp( final String ip ) throws NoSuchElementException {
    EntityTransaction db = Entities.get( VmInstance.class );
    try {
      VmInstance vmExample = VmInstance.exampleWithPrivateIp( ip );
      VmInstance vm = ( VmInstance ) Entities.createCriteriaUnique( VmInstance.class )
                                             .add( Example.create( vmExample ).enableLike( MatchMode.EXACT ) )
                                             .add( Restrictions.in( "state", new VmState[] { VmState.RUNNING, VmState.PENDING } ) )
                                             .uniqueResult( );
      if ( vm == null ) {
        throw new NoSuchElementException( "VmInstance with private ip: " + ip );
      }
      db.commit( );
      return vm;
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw new NoSuchElementException( ex.getMessage( ) );
    }
  }
  
  public static VmVolumeAttachment lookupVolumeAttachment( final String volumeId ) {
    VmVolumeAttachment ret = null;
    EntityTransaction db = Entities.get( VmInstance.class );
    try {
      List<VmInstance> vms = Entities.query( VmInstance.create( ) );
      for ( VmInstance vm : vms ) {
        try {
          ret = vm.lookupVolumeAttachment( volumeId );
          if ( ret.getVmInstance( ) == null ) {
            ret.setVmInstance( vm );
          }
        } catch ( NoSuchElementException ex ) {
          continue;
        }
      }
      if ( ret == null ) {
        throw new NoSuchElementException( "VmVolumeAttachment: no volume attachment for " + volumeId );
      }
      db.commit( );
      return ret;
    } catch ( Exception ex ) {
      db.rollback( );
      throw new NoSuchElementException( ex.getMessage( ) );
    }
    
  }
  
  public static VmInstance lookupByPublicIp( final String ip ) throws NoSuchElementException {
    EntityTransaction db = Entities.get( VmInstance.class );
    try {
      VmInstance vmExample = VmInstance.exampleWithPublicIp( ip );
      VmInstance vm = ( VmInstance ) Entities.createCriteriaUnique( VmInstance.class )
                                             .add( Example.create( vmExample ).enableLike( MatchMode.EXACT ) )
                                             .add( Restrictions.in( "state", new VmState[] { VmState.RUNNING, VmState.PENDING } ) )
                                             .uniqueResult( );
      if ( vm == null ) {
        throw new NoSuchElementException( "VmInstance with public ip: " + ip );
      }
      db.commit( );
      return vm;
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      db.rollback( );
      throw new NoSuchElementException( ex.getMessage( ) );
    }
  }
  
  public static Predicate<VmInstance> withBundleId( final String bundleId ) {
    return new Predicate<VmInstance>( ) {
      @Override
      public boolean apply( final VmInstance vm ) {
        return ( vm.getRuntimeState( ).getBundleTask( ) != null ) && vm.getRuntimeState( ).getBundleTask( ).getBundleId( ).equals( bundleId );
      }
    };
  }
  
  public static VmInstance lookupByBundleId( final String bundleId ) throws NoSuchElementException {
    return Iterables.find( list( ), withBundleId( bundleId ) );
  }
  
  public static void cleanUp( final VmInstance vm ) {
    VmState vmLastState = vm.getLastState( );
    VmState vmState = vm.getState( );
    RuntimeException logEx = new RuntimeException( "Cleaning up instance: " + vm.getInstanceId( ) + " " + vmLastState + " -> " + vmState );
    LOG.debug( logEx.getMessage( ) );
    Logs.extreme( ).info( logEx, logEx );
    try {
      if ( NetworkGroups.networkingConfiguration( ).hasNetworking( ) ) {
        try {
          Address address = Addresses.getInstance( ).lookup( vm.getPublicAddress( ) );
          if ( ( address.isAssigned( ) && vm.getInstanceId( ).equals( address.getInstanceId( ) ) ) //assigned to this instance explicitly
               || VmState.PENDING.equals( vmLastState ) ) { //partial assignment implicitly associated with this failed (PENDING->SHUTTINGDOWN) instance
            if ( address.isSystemOwned( ) ) {
              EventRecord.caller( VmInstances.class, EventType.VM_TERMINATING, "SYSTEM_ADDRESS", address.toString( ) ).debug( );
            } else {
              EventRecord.caller( VmInstances.class, EventType.VM_TERMINATING, "USER_ADDRESS", address.toString( ) ).debug( );
            }
            AsyncRequests.newRequest( address.unassign( ).getCallback( ) ).dispatch( vm.getPartition( ) );
          }
        } catch ( final NoSuchElementException e ) {
          //PENDING->SHUTTINGDOWN might happen before address info reported in describe instances by CC, need to try finding address
          if ( VmState.PENDING.equals( vmLastState ) ) {
            for ( Address addr : Addresses.getInstance( ).listValues( ) ) {
              if ( addr.getInstanceId( ).equals( vm.getInstanceId( ) ) ) {
                AsyncRequests.newRequest( addr.unassign( ).getCallback( ) ).dispatch( vm.getPartition( ) );
                break;
              }
            }
          }
        } catch ( final Exception e1 ) {
          LOG.debug( e1, e1 );
        }
      }
    } catch ( final Exception e ) {
      LOG.error( e );
      Logs.extreme( ).error( e, e );
    }
    try {
      VmInstances.cleanUpAttachedVolumes( vm );
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
    try {
      AsyncRequests.newRequest( new TerminateCallback( vm.getInstanceId( ) ) ).dispatch( vm.getPartition( ) );
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
  }
  
  private static final Predicate<AttachedVolume> anyVolumePred = new Predicate<AttachedVolume>( ) {
                                                                 @Override
                                                                 public boolean apply( final AttachedVolume arg0 ) {
                                                                   return true;
                                                                 }
                                                               };
  
  private static void cleanUpAttachedVolumes( final VmInstance vm ) {
    try {
      vm.eachVolumeAttachment( new Predicate<VmVolumeAttachment>( ) {
        @Override
        public boolean apply( final VmVolumeAttachment arg0 ) {
          try {
            
            if ( VmStateSet.TERM.apply( vm ) && !"/dev/sda1".equals( arg0.getDevice( ) ) ) {
              try {
                arg0.getVmInstance( ).getTransientVolumeState( ).removeVolumeAttachment( arg0.getVolumeId( ) );
              } catch ( NoSuchElementException ex ) {
                Logs.extreme( ).debug( ex );
              }
            }
            
            try {
              final ServiceConfiguration sc = Topology.lookup( Storage.class, vm.lookupPartition( ) );
              AsyncRequests.sendSync( sc, new DetachStorageVolumeType( arg0.getVolumeId( ) ) );
            } catch ( Exception ex ) {
              LOG.debug( ex );
              Logs.extreme( ).debug( ex, ex );
            }
            
            try {
              //ebs with either default deleteOnTerminate or user specified deleteOnTerminate and TERMINATING instance
              if ( VmStateSet.TERM.apply( vm ) && arg0.getDeleteOnTerminate( ) ) {
                final ServiceConfiguration sc = Topology.lookup( Storage.class, vm.lookupPartition( ) );
                AsyncRequests.dispatch( sc, new DeleteStorageVolumeType( arg0.getVolumeId( ) ) );
                Volumes.lookup( null, arg0.getVolumeId( ) ).setState( State.ANNIHILATING );
              }
            } catch ( Exception ex ) {
              LOG.debug( ex );
              Logs.extreme( ).debug( ex, ex );
            }
            
            return true;
          } catch ( final Exception e ) {
            LOG.error( "Failed to clean up attached volume: "
                       + arg0.getVolumeId( )
                       + " for instance "
                       + vm.getInstanceId( )
                       + ".  The request failed because of: "
                       + e.getMessage( ), e );
            return true;
          }
        }
      } );
    } catch ( final Exception ex ) {
      LOG.error( "Failed to lookup Storage Controller configuration for: " + vm.getInstanceId( ) + " (placement=" + vm.getPartition( ) + ").  " );
    }
  }
  
  public static String asMacAddress( final String instanceId ) {
    return String.format( "%s:%s:%s:%s:%s",
                          VmInstances.MAC_PREFIX,
                          instanceId.substring( 2, 4 ),
                          instanceId.substring( 4, 6 ),
                          instanceId.substring( 6, 8 ),
                          instanceId.substring( 8, 10 ) );
  }
  
  public static VmInstance cachedLookup( final String name ) throws NoSuchElementException, TerminatedInstanceException {
    return CachedLookup.INSTANCE.apply( name );
  }
  
  public static VmInstance lookup( final String name ) throws NoSuchElementException, TerminatedInstanceException {
    return PersistentLookup.INSTANCE.apply( name );
  }
  
  public static VmInstance register( final VmInstance vm ) {
    if ( !terminateDescribeCache.containsKey( vm.getInstanceId( ) ) ) {
      return Transitions.REGISTER.apply( vm );
    } else {
      throw new IllegalArgumentException( "Attempt to register instance which is already terminated." );
    }
  }
  
  public static VmInstance delete( final VmInstance vm ) throws TransactionException {
    try {
      if ( VmStateSet.DONE.apply( vm ) ) {
        delete( vm.getInstanceId( ) );
      }
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
    }
    return vm;
  }
  
  public static void delete( final String instanceId ) {
    try {
      Entities.asTransaction( VmInstance.class, new Function<String, String>( ) {
        
        @Override
        public String apply( String input ) {
          EntityTransaction db = Entities.get( VmInstance.class );
          try {
            VmInstance entity = Entities.uniqueResult( VmInstance.named( null, input ) );
            entity.cleanUp( );
            Entities.delete( entity );
            db.commit( );
          } catch ( final Exception ex ) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
            db.rollback( );
          }
          return input;
        }
      }, VmInstances.TX_RETRIES ).apply( instanceId );
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
    terminateDescribeCache.remove( instanceId );
    terminateCache.remove( instanceId );
  }
  
  static void cache( final VmInstance vm ) {
    if ( !terminateDescribeCache.containsKey( vm.getDisplayName( ) ) ) {
      vm.setState( VmState.TERMINATED );
      final RunningInstancesItemType ret = VmInstances.transform( vm );
      terminateDescribeCache.put( vm.getDisplayName( ), ret );
      terminateCache.put( vm.getDisplayName( ), vm );
      Entities.asTransaction( VmInstance.class, Transitions.DELETE, VmInstances.TX_RETRIES ).apply( vm );
    }
  }
  
  public static void terminated( final VmInstance vm ) throws TransactionException {
    VmInstances.cache( Entities.asTransaction( VmInstance.class, Transitions.TERMINATED, VmInstances.TX_RETRIES ).apply( vm ) );
  }
  
  public static void terminated( final String key ) throws NoSuchElementException, TransactionException {
    terminated( VmInstance.Lookup.INSTANCE.apply( key ) );
  }
  
  public static void stopped( final VmInstance vm ) throws TransactionException {
    Entities.asTransaction( VmInstance.class, Transitions.STOPPED, VmInstances.TX_RETRIES ).apply( vm );
  }
  
  public static void stopped( final String key ) throws NoSuchElementException, TransactionException {
    VmInstance vm = VmInstance.Lookup.INSTANCE.apply( key );
    if ( vm.getBootRecord( ).getMachine( ) instanceof BlockStorageImageInfo ) {
      VmInstances.stopped( vm );
    }
  }
  
  public static void shutDown( final VmInstance vm ) throws TransactionException {
    if ( !VmStateSet.DONE.apply( vm ) ) {
//      if ( terminateDescribeCache.containsKey( vm.getDisplayName( ) ) ) {
//        VmInstances.delete( vm );
//      } else {
//        VmInstances.terminated( vm );
//      }
//    } else {
      Entities.asTransaction( VmInstance.class, Transitions.SHUTDOWN, VmInstances.TX_RETRIES ).apply( vm );
    }
  }
  
  public static List<VmInstance> list( ) {
    return list( null );
  }
  
  public static List<VmInstance> list( Predicate<VmInstance> predicate ) {
    return list( null, null, predicate );
  }
  
  public static List<VmInstance> list( OwnerFullName ownerFullName, Predicate<VmInstance> predicate ) {
    return list( ownerFullName, null, predicate );
  }
  
  public static List<VmInstance> list( String instanceId, Predicate<VmInstance> predicate ) {
    return list( null, instanceId, predicate );
  }
  
  public static List<VmInstance> list( OwnerFullName ownerFullName, String instanceId, Predicate<VmInstance> predicate ) {
    predicate = checkPredicate( predicate );
    List<VmInstance> ret = listPersistent( ownerFullName, instanceId, predicate );
    ret.addAll( Collections2.filter( terminateCache.values( ), predicate ) );
    return ret;
  }
  
  private static List<VmInstance> listPersistent( OwnerFullName ownerFullName, String instanceId, Predicate<VmInstance> predicate ) {
    predicate = checkPredicate( predicate );
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final Iterable<VmInstance> vms = Iterables.filter( Entities.query( VmInstance.named( ownerFullName, instanceId ) ), predicate );
      db.commit( );
      return Lists.newArrayList( vms );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      db.rollback( );
      return Lists.newArrayList( );
    }
  }
  
  private static Predicate<VmInstance> checkPredicate( Predicate<VmInstance> predicate ) {
    if ( predicate == null ) {
      predicate = new Predicate<VmInstance>( ) {
        
        @Override
        public boolean apply( VmInstance input ) {
          return true;
        }
      };
    }
    return predicate;
  }
  
  public static boolean contains( final String name ) {
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, name ) );
      db.commit( );
      return true;
    } catch ( final RuntimeException ex ) {
      db.rollback( );
      return false;
    } catch ( final TransactionException ex ) {
      db.rollback( );
      return false;
    }
  }
  
  /**
   * @param vm
   * @return
   */
  public static RunningInstancesItemType transform( final VmInstance vm ) {
    if ( terminateDescribeCache.containsKey( vm.getDisplayName( ) ) ) {
      return terminateDescribeCache.get( vm.getDisplayName( ) );
    } else {
      return VmInstance.Transform.INSTANCE.apply( vm );
    }
  }
  
  enum PersistentLookup implements Function<String, VmInstance> {
    INSTANCE;
    
    /**
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    @Override
    public VmInstance apply( final String name ) {
      if ( ( name != null ) && VmInstances.terminateDescribeCache.containsKey( name ) ) {
        throw new TerminatedInstanceException( name );
      } else {
        return VmInstance.Lookup.INSTANCE.apply( name );
      }
    }
    
  }
  
  @Resolver( VmInstanceMetadata.class )
  public enum CachedLookup implements Function<String, VmInstance> {
    INSTANCE;
    
    /**
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    @Override
    public VmInstance apply( final String name ) {
      VmInstance vm = null;
      if ( ( name != null ) ) {
        vm = VmInstances.terminateCache.get( name );
        if ( vm == null ) {
          vm = PersistentLookup.INSTANCE.apply( name );
        }
      }
      return vm;
    }
    
  }
  
  /**
   * @param vm
   * @return
   */
  public static RunningInstancesItemType transform( final String name ) {
    if ( terminateDescribeCache.containsKey( name ) ) {
      return terminateDescribeCache.get( name );
    } else {
      return VmInstance.Transform.INSTANCE.apply( lookup( name ) );
    }
  }
  
}
