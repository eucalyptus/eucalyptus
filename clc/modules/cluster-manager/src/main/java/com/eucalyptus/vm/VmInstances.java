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

package com.eucalyptus.vm;

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment.deleteOnTerminateFilter;
import static com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment.volumeIdFilter;
import static com.eucalyptus.util.Parameters.checkParam;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.xbill.DNS.Name;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.Volumes;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.DeleteStorageVolumeType;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cloud.VmInstanceLifecycleHelper;
import com.eucalyptus.cloud.VmInstanceToken;
import com.eucalyptus.cloud.run.Allocations;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.callback.StartInstanceCallback;
import com.eucalyptus.cluster.callback.StopInstanceCallback;
import com.eucalyptus.cluster.callback.TerminateCallback;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.cluster.common.msgs.AttachedVolume;
import com.eucalyptus.cluster.common.msgs.ClusterStartInstanceType;
import com.eucalyptus.cluster.common.msgs.ClusterStopInstanceType;
import com.eucalyptus.cluster.common.msgs.NetworkConfigType;
import com.eucalyptus.cluster.common.msgs.VmInfo;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.BlockDeviceMappingItemType;
import com.eucalyptus.compute.common.CloudMetadata.VmInstanceMetadata;
import com.eucalyptus.compute.common.CloudMetadataLimitedType;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.DeleteResourceTag;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.compute.common.InstanceStatusEventType;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.ResourceTagMessage;
import com.eucalyptus.compute.common.backend.CreateTagsType;
import com.eucalyptus.compute.common.backend.DeleteTagsType;
import com.eucalyptus.compute.common.backend.StopInstancesType;
import com.eucalyptus.compute.common.backend.TerminateInstancesType;
import com.eucalyptus.compute.common.internal.account.IdentityIdFormats;
import com.eucalyptus.compute.common.internal.blockstorage.State;
import com.eucalyptus.compute.common.internal.blockstorage.Volume;
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.compute.common.internal.images.BootableImageInfo;
import com.eucalyptus.compute.common.internal.images.ImageInfo;
import com.eucalyptus.compute.common.internal.images.KernelImageInfo;
import com.eucalyptus.compute.common.internal.images.RamdiskImageInfo;
import com.eucalyptus.compute.common.internal.keys.KeyPairs;
import com.eucalyptus.compute.common.internal.keys.SshKeyPair;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.compute.common.internal.vm.MigrationState;
import com.eucalyptus.compute.common.internal.vm.VmBootRecord;
import com.eucalyptus.compute.common.internal.vm.VmBootVolumeAttachment;
import com.eucalyptus.compute.common.internal.vm.VmBundleTask;
import com.eucalyptus.compute.common.internal.vm.VmEphemeralAttachment;
import com.eucalyptus.compute.common.internal.vm.VmId;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmStateSet;
import com.eucalyptus.compute.common.internal.vm.VmInstanceTag;
import com.eucalyptus.compute.common.internal.vm.VmLaunchRecord;
import com.eucalyptus.compute.common.internal.vm.VmMigrationTask;
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig;
import com.eucalyptus.compute.common.internal.vm.VmPlacement;
import com.eucalyptus.compute.common.internal.vm.VmRuntimeState;
import com.eucalyptus.compute.common.internal.vm.VmStandardVolumeAttachment;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment;
import com.eucalyptus.compute.common.internal.vm.VmVolumeState;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaces;
import com.eucalyptus.compute.common.internal.vpc.Subnet;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.PersistenceExceptions;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionExecutionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.TransientEntityException;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.images.Emis;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.tracking.MessageContexts;
import com.eucalyptus.tags.TagHelper;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Intervals;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.MessageCallback;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.CaseFormat;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Suppliers;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ConfigurableClass( root = "cloud.vmstate",
                    description = "Parameters controlling the lifecycle of virtual machines." )
public class VmInstances extends com.eucalyptus.compute.common.internal.vm.VmInstances {

  private static final Logger LOG = Logger.getLogger( VmInstances.class );

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
    UNTOUCHED( VmState.PENDING, VmState.RUNNING ) {
      @Override
      public Integer getMinutes( ) {
        return INSTANCE_TOUCH_INTERVAL;
      }
    },
    UNREPORTED( VmState.PENDING, VmState.RUNNING ) {
      @Override
      public Integer getMinutes( ) {
        return (int) TimeUnit.MINUTES.convert(
            Intervals.parse( INSTANCE_TIMEOUT, TimeUnit.MINUTES, TimeUnit.DAYS.toMillis( 180 ) ),
            TimeUnit.MILLISECONDS );
      }
    },
    PENDING( VmState.PENDING ) {
      @Override
      public Integer getMinutes( ) {
        return PENDING_TIME;
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
    },
    BURIED( VmState.BURIED ) {
      @Override
      public Integer getMinutes( ) {
        return BURIED_TIME;
      }
    },
    ;

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


  enum Transitions implements Function<VmInstance, VmInstance> {
    BURIED {
      @Override
      public VmInstance apply( final VmInstance v ) {
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          if ( VmState.TERMINATED.apply( vm ) ) {
            vm.setState( VmState.BURIED );
          }
          return vm;
        } catch ( final Exception ex ) {
          Logs.extreme( ).trace( ex, ex );
          throw new NoSuchElementException( "Failed to lookup instance: " + v );
        }
      }
    },
    TERMINATED {
      @Override
      public VmInstance apply( final VmInstance v ) {
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          final boolean pendingTimeout = Timeout.PENDING.apply( vm );
          final boolean unreportedTimeout = Timeout.UNREPORTED.apply( vm );
          final Pair<VmState,VmInstance.Reason> stateAndReason;
          if ( VmStateSet.RUN.apply( vm ) && unreportedTimeout ) {
            stateAndReason = Pair.pair( VmState.SHUTTING_DOWN, VmInstance.Reason.EXPIRED );
          } else if ( VmStateSet.RUN.apply( vm ) && pendingTimeout ) {
            stateAndReason = Pair.pair( VmState.TERMINATED, VmInstance.Reason.FAILED );
          } else if ( VmStateSet.RUN.apply( vm ) ) {
            stateAndReason = Pair.pair( VmState.SHUTTING_DOWN, VmInstance.Reason.USER_TERMINATED );
          } else if ( VmState.SHUTTING_DOWN.equals( vm.getState( ) ) && ( Timeout.SHUTTING_DOWN.apply( vm ) || VmInstance.Reason.EXPIRED.apply( vm ) ) ) {
            stateAndReason = Pair.pair( VmState.TERMINATED, VmInstance.Reason.EXPIRED );
          } else if ( VmState.SHUTTING_DOWN.equals( vm.getState( ) ) ) {
            stateAndReason = Pair.pair( VmState.TERMINATED, VmInstance.Reason.USER_TERMINATED );
          } else if ( VmState.STOPPED.equals( vm.getState( ) ) ) {
            stateAndReason = Pair.pair( VmState.TERMINATED, VmInstance.Reason.USER_TERMINATED );
          } else {
            stateAndReason = null;
          }
          if ( stateAndReason != null ) {
            setState( vm, stateAndReason.getLeft( ), stateAndReason.getRight( ) );
          }
          return vm;
        } catch ( final Exception ex ) {
          Logs.extreme( ).trace( ex, ex );
          throw new NoSuchElementException( "Failed to lookup instance: " + v );
        }
      }
    },
    STOPPED {
      @Override
      public VmInstance apply( final VmInstance v ) {
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          if ( VmStateSet.RUN.apply( vm ) ) {
            setState( vm, VmState.STOPPING, VmInstance.Reason.USER_STOPPED );
          } else if ( VmState.STOPPING.equals( vm.getState( ) ) ) {
            setState( vm, VmState.STOPPED, VmInstance.Reason.USER_STOPPED );
          }
          return vm;
        } catch ( final Exception ex ) {
          Logs.extreme( ).debug( ex, ex );
          throw new NoSuchElementException( "Failed to lookup instance: " + v );
        }
      }
    },
    DELETE {
      @Override
      public VmInstance apply( final VmInstance v ) {
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          Entities.delete( vm );
        } catch ( final Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
        }
        v.setState( VmState.TERMINATED );
        return v;
      }
    },
    SHUTDOWN {
      @Override
      public VmInstance apply( final VmInstance v ) {
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          if ( VmStateSet.RUN.apply( vm ) ) {
            VmInstance.Reason reason = Timeout.SHUTTING_DOWN.apply( vm ) ? VmInstance.Reason.EXPIRED : VmInstance.Reason.USER_TERMINATED;
            setState( vm, VmState.SHUTTING_DOWN, reason );
          }
          return vm;
        } catch ( final Exception ex ) {
          Logs.extreme( ).debug( ex, ex );
          throw new NoSuchElementException( "Failed to lookup instance: " + v );
        }
      }
    };
    @Override
    public abstract VmInstance apply( final VmInstance v );
  }

  public static class VmSpecialUserData {
    /*
     * Format:
     * First line must be EUCAKEY_CRED_SETUP:expirationDays
     * The rest is payload
     */
    public static final String EUCAKEY_CRED_SETUP = "euca-"+B64.standard.encString("setup-credential");
    
    private String key = null;
    private String payload = null;
    private String userData = null;
    private String expirationDays = null;
    
    public VmSpecialUserData(final String userData) {
      if (userData == null || userData.isEmpty())
        return;
      int i = userData.indexOf("\n");
      if (i < EUCAKEY_CRED_SETUP.length())
        return;
      final String[] firstLine = userData.substring(0, i).split(":");
      if (firstLine.length != 2){
        LOG.error("Invalid credentials format");
        return;
      }
      this.key = firstLine[0];
      this.expirationDays = firstLine[1];
      this.payload = userData.substring(i+1);
      this.userData = userData;
    }
    
    @Override
    public String toString() {
      return this.userData;
    }

    public String getPayload() {
      return this.payload;
    }
    
    public String getKey() {
      return this.key;
    }
    
    public String getExpirationDays() {
      return this.expirationDays;
    }
    
    public static boolean apply(final String userData) {
      if(userData == null || 
          userData.length()< EUCAKEY_CRED_SETUP.length())
        return false;
      if(!userData.startsWith(EUCAKEY_CRED_SETUP))
        return false;
      
      return true;
    }
  }
  
  public static class UserDataMaxSizeChangeListener implements PropertyChangeListener {
    /**
     * @see com.eucalyptus.configurable.PropertyChangeListener#fireChange(com.eucalyptus.configurable.ConfigurableProperty,
     *      java.lang.Object)
     */
    @Override
    public void fireChange(final ConfigurableProperty t, final Object newValue)
        throws ConfigurablePropertyException {
      LOG.info("in fire change");
      int maxSizeKB = -1;
      try {
        if (newValue == null) {
          throw new NullPointerException("newValue");
        } else if (newValue instanceof String) {
          maxSizeKB = Integer.parseInt((String) newValue);
        } else if (newValue instanceof Integer) {
          maxSizeKB = (Integer) newValue;
        } else {
          throw new ClassCastException("Expecting Integer or String for value, got " + newValue.getClass());
        }
      } catch (Exception ex) {
        throw new ConfigurablePropertyException("Invalid value " + newValue);
      }
      if (maxSizeKB <=0 || maxSizeKB > ABSOLUTE_USER_DATA_MAX_SIZE_KB) {
        throw new ConfigurablePropertyException("Invalid value " + newValue + ", must be between 1 and " + ABSOLUTE_USER_DATA_MAX_SIZE_KB);
      }
      try {
        t.getField().set(null, t.getTypeParser().apply(newValue.toString()));
      } catch (IllegalArgumentException e1) {
        e1.printStackTrace();
        throw new ConfigurablePropertyException(e1);
      } catch (IllegalAccessException e1) {
        e1.printStackTrace();
        throw new ConfigurablePropertyException(e1);
      }
    }
  }
  @ConfigurableField( description = "Max length (in KB) that the user data file can be for an instance (after base 64 decoding) " ,
      initial = "16" , changeListener = UserDataMaxSizeChangeListener.class )
  public static String USER_DATA_MAX_SIZE_KB = "16";
  public static final Integer ABSOLUTE_USER_DATA_MAX_SIZE_KB = 64; // This may need to be related to chunk size in the future

  @ConfigurableField( description = "Number of times to retry transactions in the face of potential concurrent update conflicts.",
                      initial = "10", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public static volatile int TX_RETRIES                 = 10;

  @ConfigurableField( description = "Amount of time (default unit minutes) before a previously running instance which is not reported will be marked as terminated.",
                      initial = "180d" )
  public static String    INSTANCE_TIMEOUT              = "180d";

  @ConfigurableField( description = "Amount of time (in minutes) between updates for a running instance.",
                      initial = "15" )
  public static Integer   INSTANCE_TOUCH_INTERVAL       = 15;

  @ConfigurableField( description = "Amount of time (in minutes) before a pending VM will be terminated.",
      initial = "60" )
  public static Integer   PENDING_TIME                  = 60;

  @ConfigurableField( description = "Amount of time (in minutes) before a VM which is not reported by a cluster will be marked as terminated.",
                      initial = "10" )
  public static Integer   SHUT_DOWN_TIME                = 10;

  @ConfigurableField( description = "Amount of time (in minutes) before a stopping VM which is not reported by a cluster will be marked as stopped.",
                      initial = "10" )
  public static Integer   STOPPING_TIME                 = 10;

  @ConfigurableField( description = "Amount of time (in minutes) that a terminated VM will continue to be reported.",
                      initial = "60" )
  public static Integer   TERMINATED_TIME               = 60;

  @ConfigurableField( description = "Amount of time (in minutes) to retain unreported terminated instance data.",
                      initial = "60" )
  public static Integer   BURIED_TIME                   = 60;

  @ConfigurableField( description = "Maximum amount of time (in seconds) that migration state will take to propagate state changes (e.g., to tags).",
                      initial = "" + 60 )
  public static Long      MIGRATION_REFRESH_TIME        = 60l;

  @ConfigurableField( description = "Default prefix to use for instance / network interface MAC addresses.",
                      initial = "d0:0d" )
  public static String    MAC_PREFIX                    = "d0:0d";

  @ConfigurableField( description = "Subdomain to use for instance DNS.",
                      initial = ".eucalyptus",
                      changeListener = SubdomainListener.class )
  public static String    INSTANCE_SUBDOMAIN            = ".eucalyptus";

  @ConfigurableField( description = "Period (in seconds) between state updates for actively changing state.",
                      initial = "9223372036854775807" )
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

  @ConfigurableField( description = "Name for root block device mapping",
                      initial = "emi",
                      changeListener = EbsRootDeviceChangeListener.class )
  public static volatile String EBS_ROOT_DEVICE_NAME    = "emi";

  @ConfigurableField( description = "Amount of time (in seconds) to let instance state settle after a transition to either stopping or shutting-down.",
                      initial = "40" )
  public static Integer   VM_STATE_SETTLE_TIME          = 40;

  @ConfigurableField( description = "Amount of time (in seconds) since completion of the creating run instance operation that the new instance is treated as unreported if not... reported.",
                      initial = "300" )
  public static Integer   VM_INITIAL_REPORT_TIMEOUT     = 300;

  @ConfigurableField( description = "Amount of time (in minutes) before a VM which is not reported by a cluster will fail a reachability test.",
      initial = "5" )
  public static Integer INSTANCE_REACHABILITY_TIMEOUT   = 5;

  @ConfigurableField( description = "Comma separated list of handlers to use for unknown instances ('restore-failed', 'terminate', 'terminate-done')",
      initial = "terminate-done, restore-failed", changeListener = UnknownInstanceHandlerChangeListener.class )
  public static String UNKNOWN_INSTANCE_HANDLERS        = "terminate-done, restore-failed";

  @ConfigurableField( description = "Instance metadata user data cache configuration.",
      initial = "maximumSize=50, expireAfterWrite=5s, softValues",
      changeListener = PropertyChangeListeners.CacheSpecListener.class )
  public static volatile String VM_METADATA_USER_DATA_CACHE   = "maximumSize=50, expireAfterWrite=5s, softValues";

  @ConfigurableField( description = "Instance metadata cache configuration.",
      initial = "maximumSize=250, expireAfterWrite=5s",
      changeListener = PropertyChangeListeners.CacheSpecListener.class )
  public static volatile String VM_METADATA_INSTANCE_CACHE    = "maximumSize=250, expireAfterWrite=5s";

  @ConfigurableField( description = "Instance metadata instance resolution cache configuration.",
      initial = "maximumSize=250, expireAfterWrite=1s",
      changeListener = PropertyChangeListeners.CacheSpecListener.class )
  public static volatile String VM_METADATA_REQUEST_CACHE     = "maximumSize=250, expireAfterWrite=1s";

  @ConfigurableField( description = "Instance metadata generated data cache configuration.",
      initial = "maximumSize=1000, expireAfterWrite=5m",
      changeListener = PropertyChangeListeners.CacheSpecListener.class )
  public static volatile String VM_METADATA_GENERATED_CACHE     = "maximumSize=1000, expireAfterWrite=5m";

  public static class SubdomainListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      
      if ( !newValue.toString( ).startsWith( "." ) || newValue.toString( ).endsWith( "." ) )
        throw new ConfigurablePropertyException( "Subdomain must begin and cannot end with a '.' -- e.g., '." + newValue.toString( ).replaceAll( "\\.$", "" )
                                                 + "' is correct." + t.getFieldName( ) );
      
    }
  }

  public static class UnknownInstanceHandlerChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
       final Iterable<Optional<RestoreHandler>> handlers =
           RestoreHandler.parseList( String.valueOf( newValue ) );
       if ( Iterables.size( handlers ) != Iterables.size( Optional.presentInstances( handlers ) ) ) {
         throw new ConfigurablePropertyException( "Invalid unknown instance handler in " + newValue + "; valid values are 'restore', 'restore-failed', 'terminate', 'terminate-done'" );
       }
    }
  }

  public static class EbsRootDeviceChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      if ( newValue != null && !"[a-zA-Z0-9]{1,128}".matches( String.valueOf( newValue ) ) ) {
        throw new ConfigurablePropertyException( "Invalid ebs root device name: " + newValue );
      }
      ebsRootDeviceName.set( String.valueOf( Objects.firstNonNull( newValue, Objects.firstNonNull( EBS_ROOT_DEVICE_NAME, "emi" ) ) ) );
    }
  }

  @QuantityMetricFunction( VmInstanceMetadata.class )
  public enum CountVmInstances implements Function<OwnerFullName, Long> {
    INSTANCE;
    
    @Override
    public Long apply( final OwnerFullName ownerFullName ) {
      return
          countPersistentInstances( ownerFullName ) +
          countPendingInstances( ownerFullName );
    }

    private long countPersistentInstances( final OwnerFullName ownerFullName ) {
      try ( TransactionResource tx = Entities.transactionFor( VmInstance.class ) ){
        return Entities.count(
            VmInstance.named( ownerFullName, null ),
            Restrictions.not(VmInstance.criterion(VmStateSet.DONE.array())),
            Collections.<String,String>emptyMap() );
      }
    }

    private long countPendingInstances( final OwnerFullName ownerFullName ) {
      long pending = 0;
      for ( final Cluster cluster : Clusters.list( ) ) {
        pending += cluster.getNodeState( ).countUncommittedPendingInstances( ownerFullName );
      }
      return pending;
    }
  }

  @RestrictedTypes.UsageMetricFunction( CloudMetadataLimitedType.VmInstanceActiveMetadata.class )
  public enum CountVmInstanceActiveNum implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName ownerFullName ) {
      return
        countPersistentInstances( ownerFullName ) +
          countPendingInstances( ownerFullName );
    }

    private long countPersistentInstances( final OwnerFullName ownerFullName ) {
      try ( TransactionResource tx = Entities.transactionFor( VmInstance.class ) ){
        return Entities.count(
          VmInstance.named( ownerFullName, null ),
          Restrictions.not( VmInstance.criterion( VmStateSet.TORNDOWN.array() ) ),
          Collections.<String,String>emptyMap( ) );
      }
    }

    private long countPendingInstances( final OwnerFullName ownerFullName ) {
      long pending = 0;
      for ( final Cluster cluster : Clusters.list( ) ) {
        pending += cluster.getNodeState( ).countUncommittedPendingInstances( ownerFullName );
      }
      return pending;
    }
  }


  public static String getId( final String identityArn ) {
    String vmId;
    do {
      vmId = IdentityIdFormats.generate( identityArn, VmInstance.ID_PREFIX );
    } while ( VmInstances.contains( vmId ) );
    return vmId;
  }
  
  public static VmInstance lookupByPrivateIp( final String ip ) throws NoSuchElementException {
    try ( TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
      VmInstance vmExample = VmInstance.exampleWithPrivateIp( ip );
      VmInstance vm = ( VmInstance ) Entities.createCriteriaUnique( VmInstance.class )
                                             .add( Example.create( vmExample ) )
                                             .add( Restrictions.in( "state", new VmState[] { VmState.RUNNING, VmState.PENDING } ) )
                                             .uniqueResult( );
      if ( vm == null ) {
        throw new NoSuchElementException( "VmInstance with private ip: " + ip );
      }
      db.commit( );
      return vm;
    } catch ( Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      throw new NoSuchElementException( ex.getMessage( ) );
    }
  }

  public static VmVolumeAttachment lookupVolumeAttachment( final String volumeId ) {
    VmVolumeAttachment ret = null;
    try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
      final List<VmInstance> vms = VmInstances.list( null,
          Restrictions.or(
              Restrictions.eq( "bootVolumes.volumeId", volumeId ),
              Restrictions.eq( "volumeAttachments.volumeId", volumeId )
          ),
          ImmutableMap.of( "transientVolumeState.attachments", "volumeAttachments", "bootRecord.persistentVolumes", "bootVolumes" ),
          null,
          true );
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
      throw new NoSuchElementException( ex.getMessage( ) );
    }
  }

  public static List<VmEphemeralAttachment> lookupEphemeralDevices(final String instanceId){
	  try ( TransactionResource db =
	          Entities.transactionFor( VmInstance.class ) ) {
		  final VmInstance vm = Entities.uniqueResult(VmInstance.named(instanceId));
		  final List<VmEphemeralAttachment> ephemeralDisks = 
				  Lists.newArrayList(vm.getBootRecord().getEphemeralStorage());
		  db.commit();
		  return ephemeralDisks;
	  }catch(NoSuchElementException ex){
		  throw ex;
	  }catch(Exception ex){
		  throw Exceptions.toUndeclared(ex);
	  }
  }

  public static Function<VmEphemeralAttachment, BlockDeviceMappingItemType> EphemeralAttachmentToDevice =
		  new Function<VmEphemeralAttachment, BlockDeviceMappingItemType>(){
	  @Override
	  @Nullable
	  public BlockDeviceMappingItemType apply(
			  @Nullable VmEphemeralAttachment input) {
		  final BlockDeviceMappingItemType item = new BlockDeviceMappingItemType();
		  item.setDeviceName( input.getDevice());
		  item.setVirtualName(input.getEphemeralId());
		  return item;
	  }
  };

  public static List<String> lookupPersistentDeviceNames(final String instanceId){
    try ( TransactionResource db =
	        Entities.transactionFor( VmInstance.class ) ) {;
      List<String> deviceNames = new ArrayList<>();
	  final VmInstance vm = Entities.uniqueResult(VmInstance.named(instanceId));
	  for(VmVolumeAttachment vol:vm.getBootRecord().getPersistentVolumes()){
        deviceNames.add(vol.getDevice());
	  }
	  db.commit();
      return deviceNames;
	}catch(NoSuchElementException ex){
	  throw ex;
	}catch(Exception ex){
	  throw Exceptions.toUndeclared(ex);
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
    return Iterables.find( list( withBundleId( bundleId ) ), withBundleId( bundleId ) );
  }

  public static void tryCleanUp( final VmInstance vm ) {
    cleanUp( vm, true );
  }

  public static void cleanUp( final VmInstance vm ) {
    cleanUp( vm, false );
  }

  private static void cleanUp( final VmInstance vm,
                               final boolean rollbackNetworkingOnFailure ) {
    BaseMessage originReq = null;
    try{
      originReq = MessageContexts.lookupLast(vm.getInstanceId(), Sets.<Class>newHashSet(
          TerminateInstancesType.class,
          StopInstancesType.class
          ));
    }catch(final Exception ex){
      ;
    }
    
    final VmState vmLastState = vm.getLastState( );
    final VmState vmState = vm.getState( );
    RuntimeException logEx = new RuntimeException( "Cleaning up instance: " + vm.getInstanceId( ) + " " + vmLastState + " -> " + vmState );
    LOG.debug( logEx.getMessage( ) );
    Logs.extreme( ).info( logEx, logEx );

    try {
      VmInstances.cleanUpAttachedVolumes( vm );
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }

    try {
       Entities.asDistinctTransaction( VmInstance.class, new Predicate<VmInstance>( ) {
         @Override
         public boolean apply( @Nullable final VmInstance vmInstance ) {
           VmInstanceLifecycleHelper.get( ).cleanUpInstance( Entities.merge( vm ), vmState );
           return true;
         }
       } ).apply( vm );
    } catch ( Exception ex ) {
      LOG.error( "Lifecycle clean up error for instance: " + vm.getInstanceId( ), ex );
      Logs.extreme( ).error( ex, ex );
    }
    
    if ( !rollbackNetworkingOnFailure && VmStateSet.TORNDOWN.apply( vm ) ) {
      clearServiceTag( vm );
      try {
        Entities.asDistinctTransaction( VmInstance.class, new Predicate<VmInstance>( ) {
          @Override
          public boolean apply( @Nullable final VmInstance vmInstance ) {
            if ( VmStateSet.DONE.apply( vm ) ) {
              Entities.merge( vm ).clearReferences( );
            } else {
              Entities.merge( vm ).clearRunReferences( );
            }
            return true;
          }
        } ).apply( vm );
      } catch ( Exception ex ) {
        LOG.error( "Error clearing references for instance: " + vm.getInstanceId( ), ex );
        Logs.extreme( ).error( ex, ex );
      }
    }

    sendTerminate( vm.getInstanceId( ), vm.getPartition( ) );
  }

  static void sendTerminate( final String instanceId, final String partition ) {
    if ( Partitions.exists( partition ) ) try {
      // Thread pool with size 4 and synchronous send ensures termination
      // requests do not back up and prevent other cluster communication
      Threads.enqueue(
          Topology.lookup( ClusterController.class, Partitions.lookupByName( partition ) ),
          VmInstances.class,
          4,
          new Callable<Void>( ) {
            private final long requested = System.currentTimeMillis( );
            @Override
            public Void call( ) throws Exception {
              if ( System.currentTimeMillis( ) > requested + ( VmInstances.VOLATILE_STATE_TIMEOUT_SEC * 1000l ) ) {
                LOG.info( "Attempted terminate timed out in queue for " + instanceId );
              } else if ( Partitions.exists( partition )  ) try {
                final TerminateCallback cb = new TerminateCallback( instanceId );
                AsyncRequests.newRequest(  cb ).sendSync(
                    Topology.lookup( ClusterController.class, Partitions.lookupByName( partition ) ) );
              } catch ( Exception ex ) {
                LOG.error( ex );
                Logs.extreme( ).error( ex, ex );
              }
              return null;
            }
          }
      );
    } catch ( Exception ex ) {
      LOG.error( ex );
      Logs.extreme( ).error( ex, ex );
    }
  }

  private static void cleanUpAttachedVolumes( final String instanceId,
                                              final String qualifier,
                                              final Collection<? extends VmVolumeAttachment> attachments,
                                              final Predicate<? super VmVolumeAttachment> matching ) {
    if ( attachments != null ) {
      for ( final VmVolumeAttachment attachment : Iterables.filter( Lists.newArrayList( attachments ), matching ) ) {
        try {
          LOG.debug( instanceId + ": Marking " + qualifier + " volume EXTANT " + attachment.getVolumeId( ) );
          final Volume volume = Volumes.lookup( null, attachment.getVolumeId( ) );
          if ( State.BUSY.equals( volume.getState( ) ) ) {
            volume.setState( State.EXTANT );
          }
          attachments.remove( attachment );
        } catch ( NoSuchElementException e ) {
          LOG.debug( instanceId + ": Unable to find " + qualifier + " volume not found for cleanup " + attachment.getVolumeId( ) );
        } catch ( Exception ex ) {
          LOG.error( instanceId + ": Failed to cleanup " + qualifier + " volume attachment for " + attachment.getVolumeId( ), ex );
        }
      }
    }
  }

  private static void addMatchingVolumeIds( final Collection<String> volumeIds,
                                            final Collection<? extends VmVolumeAttachment> attachments,
                                            final Predicate<? super VmVolumeAttachment> matching ) {
    CollectionUtils.fluent( attachments )
        .filter( matching )
        .transform( VmVolumeAttachment.volumeId( ) )
        .copyInto( volumeIds );
  }

  // EUCA-6935 Changing the way attached volumes are cleaned up.
  private static void cleanUpAttachedVolumes( final VmInstance vm ) {
    if ( VmStateSet.DONE.apply( vm ) ) {
      final Collection<String> volumesToDelete = Sets.newTreeSet();
      try ( final TransactionResource db = Entities.distinctTransactionFor( VmInstance.class ) ) {
        final VmInstance instance = Entities.merge( vm );

        // Clean up transient volumes
        if ( instance.getTransientVolumeState( ) != null ) {
          cleanUpAttachedVolumes( instance.getInstanceId( ), "transient", instance.getTransientVolumeState( ).getAttachments( ), deleteOnTerminateFilter( false ) );
          addMatchingVolumeIds( volumesToDelete, instance.getTransientVolumeState().getAttachments(), deleteOnTerminateFilter( true ) );
        }

        // Clean up persistent volumes that are not delete-on-terminate
        if ( instance.getBootRecord() != null ) {
          cleanUpAttachedVolumes( instance.getInstanceId( ), "persistent", instance.getBootRecord( ).getPersistentVolumes( ), deleteOnTerminateFilter( false ) );
          addMatchingVolumeIds( volumesToDelete, instance.getBootRecord( ).getPersistentVolumes( ), deleteOnTerminateFilter( true ) );
        }

        db.commit( );
      } catch ( Exception ex ) {
        LOG.error( vm.getInstanceId() + ": Failed to cleanup attached volumes", ex );
      }

      try {
        if ( !volumesToDelete.isEmpty( ) ) {
          LOG.debug( vm.getInstanceId() + ": Cleanup for delete on terminate volumes." );
          for ( final String volumeId : volumesToDelete ) {
            try {
              LOG.debug( vm.getInstanceId() + ": Firing delete request for " + volumeId );
              AsyncRequests.newRequest( new MessageCallback<DeleteStorageVolumeType,DeleteStorageVolumeResponseType>( new DeleteStorageVolumeType( volumeId ) ){
                @Override
                public void initialize( final DeleteStorageVolumeType request ) { }

                @Override
                public void fire( final DeleteStorageVolumeResponseType response ) {
                  Function<DeleteStorageVolumeResponseType,Void> deleteVolume = new Function<DeleteStorageVolumeResponseType,Void>(){
                    @Nullable
                    @Override
                    public Void apply( final DeleteStorageVolumeResponseType deleteStorageVolumeResponseType ) {
                      final Volume volume = Volumes.lookup( null, volumeId );
                      if ( null != response && response.get_return( ) ) {
                        Volumes.annihilateStorageVolume( volume );
                      } else {
                        LOG.error( vm.getInstanceId() + ": Failed to delete volume " +volumeId );
                      }
                      VmInstance instance = Entities.merge( vm );
                      if ( instance.getTransientVolumeState() != null && instance.getTransientVolumeState().getAttachments() != null ) {
                        Iterables.removeIf( instance.getTransientVolumeState().getAttachments(), volumeIdFilter( volumeId ) );
                      }
                      if ( instance.getBootRecord() != null && instance.getBootRecord().getPersistentVolumes() != null ) {
                        Iterables.removeIf( instance.getBootRecord().getPersistentVolumes(), volumeIdFilter( volumeId ) );
                      }
                      return null;
                    }
                  };
                  Entities.asTransaction( Volume.class, deleteVolume ).apply( response );
                }

                @Override
                public void fireException( final Throwable throwable ) {
                  LOG.error( vm.getInstanceId() + ": Failed to delete volume " + volumeId, throwable );
                }
              } ).dispatch( Topology.lookup( Storage.class, vm.lookupPartition() ) );
            } catch ( NoSuchElementException e ) {
              LOG.debug( vm.getInstanceId( ) + ": Persistent volume not found for cleanup " + volumeId );
            } catch ( Exception ex ) {
              LOG.error( vm.getInstanceId() + ": Failed to cleanup persistent volume attachment for " + volumeId, ex );
            }
          }
        }
      } catch ( Exception ex ) {
        LOG.error( vm.getInstanceId() + ": Failed to cleanup attached volumes", ex );
      }
    }
  }



  public static Predicate<VmInstance> initialize() {
    return InstanceInitialize.INSTANCE;
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
    final Function<String, String> delete = Entities.asTransaction( VmInstance.class, new Function<String, String>( ) {
      @Override
      public String apply( String input ) {
        try {
          VmInstance entity = Entities.uniqueResult( VmInstance.named( null, input ) );
          Entities.delete( entity );
        } catch ( final NoSuchElementException ex ) {
          LOG.debug( "Instance not found for deletion: " + instanceId );
          Logs.extreme( ).error( ex, ex );
        } catch ( final Exception ex ) {
          LOG.error( "Error deleting instance: " + instanceId + "; " + ex );
          Logs.extreme( ).error( ex, ex );
        }
        return input;
      }
    }, VmInstances.TX_RETRIES );
    try {
      delete.apply( instanceId );
    } catch ( Exception ex ) {
      if ( PersistenceExceptions.classify( ex ) == PersistenceExceptions.ErrorCategory.CONSTRAINT ) {
        LOG.error( "Error deleting instance (" + instanceId + ") due to constraints, retrying after clean up", ex );
        try {
          cleanUp( lookupAny( instanceId ) );
          delete.apply( instanceId );
        } catch ( Exception ex2 ) {
          LOG.error( "Error deleting instance (" + instanceId + ") :" + ex2 );
          Logs.extreme( ).error( ex2, ex2 );
        }
      } else {
        LOG.error( "Error deleting instance (" + instanceId + ") :" + ex );
        Logs.extreme( ).error( ex, ex );
      }
    }
  }

  public static void buried( final VmInstance vm ) throws TransactionException {
    Entities.asTransaction( VmInstance.class, Transitions.BURIED, VmInstances.TX_RETRIES ).apply( vm );
  }

  public static void buried( final String key ) throws NoSuchElementException, TransactionException {
    buried( VmInstance.Lookup.INSTANCE.apply( key ) );
  }

  public static void terminated( final VmInstance vm ) throws TransactionException {
    Entities.asTransaction( VmInstance.class, Transitions.TERMINATED, VmInstances.TX_RETRIES ).apply( vm );
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
      Entities.asTransaction( VmInstance.class, Transitions.SHUTDOWN, VmInstances.TX_RETRIES ).apply( vm );
    }
  }

  public static void reachable( final VmInstance vm ) throws TransactionException {
    transactional( InstanceStatusUpdate.REACHABLE ).apply( vm );
  }

  public static void unreachable( final VmInstance vm ) throws TransactionException {
    transactional( InstanceStatusUpdate.UNREACHABLE ).apply( vm );
  }

  private static Function<VmInstance,VmInstance> transactional( final Function<VmInstance,VmInstance> update ) {
    return Entities.asTransaction( VmInstance.class, update, VmInstances.TX_RETRIES );
  }

  public static boolean contains( final String name ) {
    final EntityTransaction db = Entities.get( VmInstance.class );
    try {
      final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, name ) );
      db.commit( );
      return true;
    } catch ( final RuntimeException ex ) {
      return false;
    } catch ( final TransactionException ex ) {
      return false;
    } finally {
      if ( db.isActive() ) db.rollback();
    }
  }

  public static Function<VmInstance,String> toNodeHost() {
    return VmInstanceFilterFunctions.NODE_HOST;
  }

  public static Function<VmInstance,String> toServiceTag() {
    return VmInstanceFilterFunctions.SERVICE_TAG;
  }

  /**
   * Caller must have open session for vm
   */
  public static void updateAddresses( final VmInstance vm, final String privateAddress, final String publicAddress ) {
    updatePrivateAddress( vm, privateAddress );
    updatePublicAddress( vm, publicAddress );
  }

  /**
   * Caller must have open session for vm
   */
  public static void updatePublicAddress( final VmInstance vm, final String publicAddress ) {
    vm.updatePublicAddress(
        ipOrDefault( publicAddress ),
        generateDnsName( publicAddress, DomainNames.externalSubdomain() )
    );
  }

  /**
   * Caller must have open session for vm
   */
  public static void updatePrivateAddress( final VmInstance vm, final String privateAddress ) {
    vm.updatePrivateAddress(
        ipOrDefault( privateAddress ),
        generateDnsName( privateAddress, DomainNames.internalSubdomain() )
    );
  }

  public static String dnsName( final String ip, final Name domain ) {
    final String suffix = domain.relativize( Name.root ).toString( );
    return "euca-" + ip.replace( '.', '-' ) + VmInstances.INSTANCE_SUBDOMAIN + "." + suffix;
  }

  public static String generateDnsName( String ip, Name domain ) {
    return VmNetworkConfig.DEFAULT_IP.equals( ip ) ?
        "" :
        dnsName( ip, domain );
  }

  private static String ipOrDefault( final String ip ) {
    return Objects.firstNonNull( com.google.common.base.Strings.emptyToNull( ip ), VmNetworkConfig.DEFAULT_IP );
  }

  public static void stopVmInstance( final VmInstance vmInstance, final StopInstanceCallback cb ) {
    final ClusterStopInstanceType request = new ClusterStopInstanceType();
    try{
      ServiceConfiguration ccConfig = Topology.lookup(ClusterController.class, vmInstance.lookupPartition());
      request.setInstanceId(vmInstance.getInstanceId());
      cb.setRequest(request);
      AsyncRequests.newRequest( cb ).dispatch(ccConfig);
    } catch (final Exception e) {
      Exceptions.toUndeclared(e);
    }
  }

  public static void startVmInstance( final VmInstance vmInstance, final StartInstanceCallback cb ) {
    final ClusterStartInstanceType request = new ClusterStartInstanceType();
    try{
      ServiceConfiguration ccConfig = Topology.lookup(ClusterController.class, vmInstance.lookupPartition());
      request.setInstanceId(vmInstance.getInstanceId());
      cb.setRequest(request);
      AsyncRequests.newRequest( cb ).dispatch(ccConfig);
    }catch (final Exception e){
      Exceptions.toUndeclared(e);
    }
  }

  /**
   *
   */
  public static void setState( final VmInstance vm, final VmState newState, VmInstance.Reason reason, final String... extra ) {
    try (TransactionResource db = Entities.transactionFor( VmInstance.class )) {
      final VmInstance entity = Entities.merge( vm );

      final VmState olderState = entity.getLastState();
      final VmState oldState = entity.getState();
      final Callable<Boolean> action;
      if ( !oldState.equals( newState ) ) {
        action = handleStateTransition( entity, newState, oldState, olderState );
      } else {
        action = null;
      }
      if ( action != null ) {
        if ( VmInstance.Reason.APPEND.equals( reason ) ) {
          reason = entity.getRuntimeState().getReason();
        }
        entity.getRuntimeState().addReasonDetail( extra );
        entity.getRuntimeState().setReason( reason );
        Entities.registerSynchronization( VmInstance.class, new Synchronization() {
          @Override
          public void beforeCompletion() {
          }

          @Override
          public void afterCompletion( final int status ) {
            if ( Status.STATUS_COMMITTED == status ) try {
              Threads.enqueue( Eucalyptus.class, VmInstance.class, VmInstances.MAX_STATE_THREADS, action );
            } catch ( final Exception ex ) {
              LOG.error( ex );
              Logs.extreme().error( ex, ex );
            }
          }
        } );
      }

      db.commit( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw Exceptions.toUndeclared( ex );
    }
  }

  /**
   * Updates VM states from DescribeInstances call
   *
   * Caller must have open session for VmInstance
   */
  public static Predicate<VmInfo> doUpdate( final VmInstance vm ) {
    return new Predicate<VmInfo>( ) {

      @Override
      public boolean apply( final VmInfo runVm ) {
        if ( !Entities.isPersistent( vm ) ) {
          throw new TransientEntityException( this.toString( ) );
        } else {
          try {
            final VmState runVmState = VmState.Mapper.get( runVm.getStateName( ) );
            if ( vm.getRuntimeState().isBundling( ) ) {
              final VmBundleTask.BundleState bundleState = VmBundleTask.BundleState.mapper.apply( runVm.getBundleTaskStateName( ) );
              Bundles.updateBundleTaskState( vm, bundleState, runVm.getBundleTaskProgress() );
            } else if ( VmStateSet.RUN.apply( vm ) && VmStateSet.RUN.contains( runVmState ) ) {
              setState( vm, runVmState, VmInstance.Reason.APPEND, "UPDATE" );
              this.updateState( runVm );
            } else if ( VmState.SHUTTING_DOWN.apply( vm ) && VmState.SHUTTING_DOWN.equals( runVmState ) ) {
              setState( vm, VmState.TERMINATED, VmInstance.Reason.APPEND, "DONE" );
            } else if ( VmInstances.Timeout.SHUTTING_DOWN.apply( vm ) ) {
              setState( vm, VmState.TERMINATED, VmInstance.Reason.EXPIRED );
            } else if ( VmInstances.Timeout.STOPPING.apply( vm ) ) {
              setState( vm, VmState.STOPPED, VmInstance.Reason.EXPIRED );
            } else if ( VmStateSet.NOT_RUNNING.apply( vm ) && VmStateSet.RUN.contains( runVmState ) ) {
              if ( Timeout.SHUTTING_DOWN.apply( vm ) ) {
                VmInstances.terminated( vm );
              } else if ( Timeout.STOPPING.apply( vm ) ) {
                VmInstances.stopped( vm );
              } else if ( vm.lastUpdateMillis() > ( VmInstances.VOLATILE_STATE_TIMEOUT_SEC * 1000l ) ) {
                VmInstances.sendTerminate( vm.getInstanceId(), vm.getPartition() );
                vm.updateTimeStamps();
              }
            } else {
              this.updateState( runVm );
            }
          } catch ( final Exception ex ) {
            Logs.extreme( ).error( ex, ex );
          }
        }
        return true;
      }

      private void updateState( final VmInfo runVm ) {
        Bundles.updateBundleTaskState( vm, runVm.getBundleTaskStateName() );
        setServiceTag( vm, runVm.getServiceTag() );
        vm.getRuntimeState().setGuestState(runVm.getGuestStateName());
        if ( !Boolean.TRUE.equals( vm.getRuntimeState().getZombie( ) ) ) {
          if ( VmStateSet.RUN.apply( vm ) ) {
            if ( !VmRuntimeState.InstanceStatus.Ok.apply( vm ) ) {
              vm.getRuntimeState().reachable( );
            }
          }
          if ( VmState.RUNNING.apply( vm ) ) {
            updateVolumeAttachments( runVm.getVolumes() );
            updateNetworkInterfaces( runVm.getSecondaryNetConfigList( ) );
            setMigrationState(
                vm,
                runVm.getMigrationStateName(),
                com.google.common.base.Strings.nullToEmpty( runVm.getMigrationSource() ),
                com.google.common.base.Strings.nullToEmpty( runVm.getMigrationDestination() ) );
          }
        }
        if ( VmInstances.Timeout.UNTOUCHED.apply( vm ) ) {
          vm.updateTimeStamps();
        }
      }

      /**
       *
       */
      private void updateVolumeAttachments( final List<AttachedVolume> volumes ) {
        try {
          final List<VmStandardVolumeAttachment> ncAttachedVols = Lists.transform( volumes, AttachedVolume.toStandardVolumeAttachment( vm ) );
          Set<String> remoteVolumes = Sets.newHashSet( Collections2.transform( ncAttachedVols, VmVolumeState.VmVolumeAttachmentName.INSTANCE ) );
          Set<String> localVolumes = Sets.newHashSet( Collections2.transform( vm.getTransientVolumeState().getAttachments(), VmVolumeState.VmVolumeAttachmentName.INSTANCE ) );
          localVolumes.addAll(Collections2.transform( vm.getBootRecord().getPersistentVolumes(), VmVolumeState.VmVolumeAttachmentName.INSTANCE ));
          Set<String> intersection = Sets.intersection( remoteVolumes, localVolumes );
          Set<String> remoteOnly = Sets.difference( remoteVolumes, localVolumes );
          Set<String> localOnly = Sets.difference( localVolumes, remoteVolumes );
          if ( !intersection.isEmpty( ) || !remoteOnly.isEmpty( ) || !localOnly.isEmpty( ) ) {
            LOG.debug( "Updating volume attachments for: " + vm.getInstanceId( )
                + " intersection=" + intersection
                + " local=" + localOnly
                + " remote=" + remoteOnly );
            LOG.debug( "Reported state for: " + vm.getInstanceId( )
                + Collections2.transform( ncAttachedVols, VmVolumeState.VmVolumeAttachmentStateInfo.INSTANCE ) );
          }
          final Map<String, VmStandardVolumeAttachment> ncAttachedVolMap = new HashMap<String, VmStandardVolumeAttachment>( ) {

            {
              for ( final VmStandardVolumeAttachment v : ncAttachedVols ) {
                this.put( v.getVolumeId( ), v );
              }
            }
          };
          for ( String volId : intersection ) {
            try {
              VmVolumeAttachment ncVolumeAttachment = ncAttachedVolMap.get( volId );
              VmVolumeAttachment localVolumeAttachment = vm.lookupVolumeAttachment( volId );
              final VmVolumeAttachment.AttachmentState localState = localVolumeAttachment.getAttachmentState( );
              final VmVolumeAttachment.AttachmentState remoteState = VmVolumeAttachment.AttachmentState.parse( ncVolumeAttachment.getStatus() );
              if ( !localState.isVolatile( ) ) {
                if ( VmVolumeAttachment.AttachmentState.detached.equals( remoteState ) ) {
                  removeVolumeAttachment( vm, volId );
                } else if ( VmVolumeAttachment.AttachmentState.attaching_failed.equals( remoteState ) ) {
                  removeVolumeAttachment( vm, volId );
                } else if ( VmVolumeAttachment.AttachmentState.detaching_failed.equals( remoteState ) && !VmVolumeAttachment.AttachmentState.attached.equals( localState ) ) {
                  updateVolumeAttachment( vm, volId, VmVolumeAttachment.AttachmentState.attached );
                } else if ( VmVolumeAttachment.AttachmentState.attached.equals( remoteState ) && !VmVolumeAttachment.AttachmentState.attached.equals( localState ) ) {
                  updateVolumeAttachment( vm, volId, VmVolumeAttachment.AttachmentState.attached );
                }
              } else {
                if ( VmVolumeAttachment.AttachmentState.detaching.equals( localState ) && VmVolumeAttachment.AttachmentState.detached.equals( remoteState ) ) {
                  removeVolumeAttachment( vm, volId );
                } else if ( VmVolumeAttachment.AttachmentState.attaching.equals( localState ) && VmVolumeAttachment.AttachmentState.attached.equals( remoteState ) ) {
                  updateVolumeAttachment( vm, volId, VmVolumeAttachment.AttachmentState.attached );
                } else if ( VmVolumeAttachment.AttachmentState.attaching.equals( localState ) && VmVolumeAttachment.AttachmentState.attaching_failed.equals( remoteState ) ) {
                  removeVolumeAttachment( vm, volId );
                } else if ( VmVolumeAttachment.AttachmentState.detaching.equals( localState ) && VmVolumeAttachment.AttachmentState.detaching_failed.equals( remoteState ) ) {
                  updateVolumeAttachment( vm, volId, VmVolumeAttachment.AttachmentState.attached );
                }
              }
            } catch ( Exception ex ) {
              LOG.error( ex );
            }
          }
          for ( String volId : remoteOnly ) {
            try {
              Volumes.lookup( null, volId );
            } catch ( NoSuchElementException e ) {
              // There is a chance that the volume was deleted and back-end does not know about that.
              // See EUCA-10453 for details
              LOG.debug("Invalid volume id " + volId + " passed from back-end");
              continue;
            }
            try {
              final VmStandardVolumeAttachment ncVolumeAttachment = ncAttachedVolMap.get( volId );
              final VmVolumeAttachment.AttachmentState remoteState = VmVolumeAttachment.AttachmentState.parse( ncVolumeAttachment.getStatus() );
              if ( VmVolumeAttachment.AttachmentState.attached.equals( remoteState ) || VmVolumeAttachment.AttachmentState.detaching_failed.equals( remoteState ) ) {
                LOG.warn( "Restoring volume attachment state for " + vm.getInstanceId( ) + " with " + ncVolumeAttachment.toString( ) );
                // swathi: how do we know if this is a transient or a persistent attachment?
                // swathi: going with transient attachment for now assuming that persistent attachments are always known to CLC since they originate in the CLC
                vm.getTransientVolumeState().addVolumeAttachment( ncVolumeAttachment );
              }
            } catch ( Exception ex ) {
              LOG.error( ex );
            }
          }
          for ( String volId : localOnly ) {
            try {
              final VmVolumeAttachment.AttachmentState localState = vm.lookupVolumeAttachment( volId ).getAttachmentState( );
              if ( !localState.isVolatile( ) ) {

              }
            } catch ( Exception ex ) {
              LOG.error( ex );
            }
          }

        } catch ( final Exception ex ) {
          Logs.extreme( ).error( ex, ex );
        }
      }

      private void updateNetworkInterfaces( final List<NetworkConfigType> networkConfigs ) {
        final Set<String> reportedAttachment = Sets.newHashSet( );
        if ( networkConfigs != null ) for ( final NetworkConfigType networkConfig : networkConfigs ) {
          reportedAttachment.add( networkConfig.getAttachmentId( ) );
        }
        boolean touch = false;
        for ( final NetworkInterface networkInterface : vm.getNetworkInterfaces( ) ) {
          if ( !networkInterface.isAttached( ) || networkInterface.getAttachment( ).getDeviceIndex( ) == 0 ) {
            continue;
          }
          final NetworkInterfaceAttachment attachment = networkInterface.getAttachment( );
          final String attachmentId = attachment.getAttachmentId( );
          if ( attachment.getStatus( ) == NetworkInterfaceAttachment.Status.attaching ) {
            if ( reportedAttachment.contains( attachmentId ) ) {
              attachment.transitionStatus( NetworkInterfaceAttachment.Status.attached );
              touch = true;
            }
          } else if ( attachment.getStatus( ) == NetworkInterfaceAttachment.Status.detaching ) {
            if ( !reportedAttachment.contains( attachmentId ) &&
                ( attachment.getLastStatus( ) == NetworkInterfaceAttachment.Status.attached ||
                  ( networkInterface.lastUpdateMillis( ) > TimeUnit.MINUTES.toMillis( 1 ) ) )
            ) {
              networkInterface.detach( );
              touch = true;
            }
          }
        }
        if ( touch ) {
          vm.updateTimeStamps( );
        }
      }
    };
  }

  public static void addTransientVolume( final VmInstance vm, final String deviceName, final String remoteDevice, final Volume vol ) {
    final Function<Volume, Volume> attachmentFunction = new Function<Volume, Volume>( ) {
      public Volume apply( final Volume input ) {
        final VmInstance entity = Entities.merge( vm );
        final Volume volEntity = Entities.merge( vol );
        final VmStandardVolumeAttachment attachVol = new VmStandardVolumeAttachment( entity, volEntity.getDisplayName( ), deviceName, remoteDevice,
            VmVolumeAttachment.AttachmentState.attaching.name( ), new Date( ), false, Boolean.FALSE );
        volEntity.setState( State.BUSY );
        entity.getTransientVolumeState( ).addVolumeAttachment( attachVol );
        return volEntity;
      }
    };
    Entities.asTransaction( VmInstance.class, attachmentFunction, VmInstances.TX_RETRIES ).apply( vol );
  }

  public static void addPersistentVolume( final VmInstance vm, final String deviceName, final Volume vol, final boolean isRootDevice, final boolean deleteOnTerminate ) {
    final Function<Volume, Volume> attachmentFunction = new Function<Volume, Volume>( ) {
      public Volume apply( final Volume input ) {
        final VmInstance entity = Entities.merge( vm );
        final Volume volEntity = Entities.merge( vol );
        // At this point the remote device string is not available. Setting this member to null leads to DB lookup issues later. So setting it to empty string instead
        final VmBootVolumeAttachment volumeAttachment = new VmBootVolumeAttachment( entity, vol.getDisplayName( ), deviceName, new String(), VmVolumeAttachment.AttachmentState.attached.name( ),
            new Date( ), deleteOnTerminate, isRootDevice, Boolean.TRUE );
        volEntity.setState( State.BUSY );
        entity.getBootRecord( ).getPersistentVolumes().add( volumeAttachment );
        return volEntity;
      }
    };
    Entities.asTransaction( VmInstance.class, attachmentFunction, VmInstances.TX_RETRIES ).apply( vol );
  }

  public static void updateAttachmentToken( final VmInstance vm, final Map<String, String> volumeAttachmentTokenMap) {
    final Function<Map<String, String>, String> updateFunction = new Function<Map<String, String>, String>() {

      @Override
      public String apply(@Nonnull Map<String, String> arg0) {
        final VmInstance entity = Entities.merge(vm);
        List<VmVolumeAttachment> allAttachments = Lists.<VmVolumeAttachment>newArrayList(entity.getBootRecord().getPersistentVolumes());
        allAttachments.addAll(entity.getTransientVolumeState().getAttachments());
        for (VmVolumeAttachment attachment : allAttachments) {
          if (arg0.containsKey(attachment.getVolumeId())) {
            attachment.setRemoteDevice(arg0.get(attachment.getVolumeId()));
          } else {
            LOG.debug("No attachment token found for " + attachment.getVolumeId() + " and " + entity.getInstanceId());
          }
        }
        return null;
      }
    };

    if (volumeAttachmentTokenMap != null && !volumeAttachmentTokenMap.isEmpty()) {
      try {
        Entities.asTransaction(VmInstance.class, updateFunction, VmInstances.TX_RETRIES).apply(volumeAttachmentTokenMap);
      } catch (Exception e) {
        LOG.warn("Failed to update attachment tokens for run time EBS volumes of " + vm.getInstanceId(), e);
      }
    } else {
      // no attachment tokens to save
    }
  }

  // Creates a DB entity associated with ephemeral devices for boot from ebs instances and stores it in the boot record
  public static void addEphemeralAttachment( final VmInstance vm, final String deviceName, final String ephemeralId ) {
    final Function<String, String> attachmentFunction = new Function<String, String>( ) {
      public String apply( final String input ) {
        final VmInstance entity = Entities.merge( vm );
        final VmEphemeralAttachment ephemeralAttachment = new VmEphemeralAttachment( entity, ephemeralId, deviceName );
        entity.getBootRecord( ).getEphemeralStorage().add( ephemeralAttachment );
        return input;
      }
    };
    Entities.asTransaction( VmInstance.class, attachmentFunction, VmInstances.TX_RETRIES ).apply( ephemeralId );
  }

  /**
   *
   */
  public static void updateVolumeAttachment( final VmInstance vm, final String volumeId, final VmVolumeAttachment.AttachmentState newState ) {
    try (TransactionResource db = Entities.transactionFor(VmInstance.class)){
      final VmInstance entity = Entities.merge( vm );
      try {
        entity.getTransientVolumeState( ).updateVolumeAttachment( volumeId, newState );
      } catch (NoSuchElementException ex) {
        VmVolumeAttachment ret = Iterables.find(entity.getBootRecord().getPersistentVolumes(), VmVolumeAttachment.volumeIdFilter(volumeId));
        ret.setStatus(newState.name());
      }
      db.commit( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
    }
  }

  /**
   *
   */
  public static VmVolumeAttachment removeVolumeAttachment( final VmInstance vm, final String volumeId ) {
    try(TransactionResource db = Entities.transactionFor( VmInstance.class )) {
      final VmInstance entity = Entities.merge( vm );
      final Volume volEntity = Volumes.lookup( null, volumeId );
      VmVolumeAttachment ret;
      try {
        ret = entity.getTransientVolumeState( ).removeVolumeAttachment( volumeId );
      } catch (NoSuchElementException ex) {
        // EUCA-5033 allow volume detachment from stopped instances
        /*if ( VmState.STOPPED.equals(entity.getState()) ) {
          ret = Iterables.find( entity.getBootRecord( ).getPersistentVolumes( ), VmVolumeAttachment.volumeIdFilter( volumeId ) );
          entity.getBootRecord( ).getPersistentVolumes( ).remove(ret);
        } else
          throw ex;*/
        // Allow detachment of persistent volumes i.e. volumes attached at run-instance time
        // Check for stopped state is performed in a higher layer - VolumeManager
        try {
          ret = Iterables.find(entity.getBootRecord().getPersistentVolumes(), VmVolumeAttachment.volumeIdFilter(volumeId));
          entity.getBootRecord().getPersistentVolumes().remove(ret);
        } catch (NoSuchElementException ex1) {
          throw ex1;
        }
      }
      if ( State.BUSY.equals( volEntity.getState( ) ) ) {
        volEntity.setState( State.EXTANT );
      }
      db.commit( );
      return ret;
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw new NoSuchElementException( "Failed to lookup volume: " + volumeId );
    }
  }

  public static void setServiceTag( final VmInstance vm, final String serviceTag ) {
    if ( serviceTag != null && !com.google.common.base.Strings.nullToEmpty( vm.getRuntimeState( ).getServiceTag( ) ).equals( serviceTag ) ) {
      vm.getRuntimeState( ).setServiceTag( serviceTag );
      setNodeTag( vm, serviceTag );
    }
  }

  public static void clearServiceTag( final VmInstance vm ) {
    if ( vm.getRuntimeState( ).getServiceTag( ) != null ) {
      vm.getRuntimeState( ).setServiceTag( null );
      clearNodeTag( vm );
    }
  }

  /**
   * Asynchronously assign the tag for this instance, do so only if it has changed.
   */
  private static void setNodeTag( final VmInstance vm, final String serviceTag2 ) {
    final String host = URI.create( serviceTag2 ).getHost();
    final CreateTagsType createTags = new CreateTagsType( );
    createTags.getTagSet( ).add( new ResourceTag( VmInstance.VM_NC_HOST_TAG, host ) );
    createTags.getResourcesSet( ).add( vm.getInstanceId( ) );
    dispatchTagMessage( createTags );
  }

  private static void clearNodeTag( final VmInstance vm ) {
    final DeleteTagsType deleteTags = new DeleteTagsType( );
    deleteTags.getTagSet( ).add( new DeleteResourceTag( VmInstance.VM_NC_HOST_TAG ) );
    deleteTags.getResourcesSet( ).add( vm.getInstanceId( ) );
    dispatchTagMessage( deleteTags );
  }

  private static void dispatchTagMessage( ResourceTagMessage message ) {
    try {
      message.setUserId( Accounts.lookupSystemAdmin( ).getUserId( ) );
      message.markPrivileged( );
      AsyncRequests.dispatch( Topology.lookup( Eucalyptus.class ), message );
    } catch ( Exception ex ) {
      LOG.error( ex );
    }
  }

  public static void startMigration( final VmInstance vm ) {
    updateMigrationTask( vm.getRuntimeState().getMigrationTask(), MigrationState.pending.name(), null, null );
    //TODO:GRZE: VolumeMigration.update( vmInstance );
    MigrationTags.update( vm );
  }

  public static void abortMigration( final VmInstance vm ) {
    updateMigrationTask( vm.getRuntimeState().getMigrationTask(), MigrationState.none.name(), null, null );
    //TODO:GRZE: VolumeMigration.update( vmInstance );
    MigrationTags.update( vm );
  }

  public static void setMigrationState( final VmInstance vm, String stateName, String sourceHost, String destHost ) {
    if ( updateMigrationTask( vm.getRuntimeState( ).getMigrationTask(), stateName, sourceHost, destHost ) ) {//actually updated the state
      //TODO:GRZE: VolumeMigration.update( vmInstance );
      MigrationTags.update( vm );
    }
  }

  /**
   * Verify and update the local state, src and dest hosts.
   */
  private static boolean updateMigrationTask( VmMigrationTask task, String state, String sourceHost, String destinationHost ) {
    MigrationState migrationState = MigrationState.defaultValueOf( state );
    /**
     * GRZE:TODO: this entire notion of refresh timer can be (and should be!) made orthogonal to the
     * domain type. Indeed, the idea that an external operation wants to have a timer associated
     * with a resource, in this case periodic tag propagation, is decidedly external state and this
     * should GTFO.
     */
    boolean timerExpired = ( System.currentTimeMillis( ) - task.getRefreshTimer( ).getTime( ) ) > TimeUnit.SECONDS.toMillis( VmInstances.MIGRATION_REFRESH_TIME );
    if ( !timerExpired && MigrationState.pending.equals( task.getState( ) ) && migrationState.ordinal( ) < MigrationState.preparing.ordinal( ) ) {
      return false;
    } else {
      boolean updated = !task.getState( ).equals( migrationState ) || !task.getSourceHost( ).equals( sourceHost ) || !task.getDestinationHost( ).equals( destinationHost );
      task.setState( migrationState );
      task.setSourceHost( sourceHost );
      task.setDestinationHost( destinationHost );
      if ( MigrationState.none.equals( task.getState( ) ) ) {
        task.setRefreshTimer( null );
        return updated || timerExpired;
      } else if ( timerExpired ) {
        task.updateRefreshTimer( );
        return true;
      } else {
        return updated;
      }
    }
  }


  /**
   * Caller must have session for given vm
   */
  private static Callable<Boolean> handleStateTransition( final VmInstance vm, final VmState newState, final VmState oldState, final VmState olderState ) {
    Callable<Boolean> action = null;
    LOG.info( String.format( "%s state change: %s -> %s (previously %s)", vm.getInstanceId(), oldState, newState, olderState ) );
    if ( VmStateSet.RUN.contains( oldState )
        && VmStateSet.NOT_RUNNING.contains( newState ) ) {
      vm.setState( newState );
      action = VmStateSet.EXPECTING_TEARDOWN.contains( newState ) ?
          tryCleanUpRunnable( vm ) : // try cleanup now, will try again when moving to final state
          cleanUpRunnable( vm );
    } else if ( VmState.PENDING.equals( oldState )
        && VmState.RUNNING.equals( newState ) ) {
      vm.setState( newState );
      if ( VmState.STOPPED.equals( olderState ) ) {
        // Fix for EUCA-6947. Skip transient volume attachment since all volumes (boot and run time) are forwarded to CC/NC at instance boot time
        // restoreVolumeState( vm );
      }
    } else if ( VmState.PENDING.equals( oldState )
        && VmState.TERMINATED.equals( newState )
        && VmState.STOPPED.equals( olderState ) ) {
      vm.setState( VmState.STOPPED );
      action = cleanUpRunnable( vm );
    } else if ( VmState.STOPPED.equals( oldState )
        && VmState.TERMINATED.equals( newState ) ) {
      vm.setState( VmState.TERMINATED );
      action = cleanUpRunnable( vm );
    } else if ( VmStateSet.EXPECTING_TEARDOWN.contains( oldState )
        && VmStateSet.RUN.contains( newState ) ) {
      vm.setState( oldState );//mask/ignore running on {stopping,shutting-down} transitions
    } else if ( VmStateSet.EXPECTING_TEARDOWN.contains( oldState )
        && VmStateSet.TORNDOWN.contains( newState ) ) {
      if ( VmState.SHUTTING_DOWN.equals( oldState ) ) {
        vm.setState( VmState.TERMINATED );
      } else {
        vm.setState( VmState.STOPPED );
      }
      action = cleanUpRunnable( vm );
    } else if ( VmState.STOPPED.equals( oldState )
        && VmState.PENDING.equals( newState ) ) {
      vm.setState( VmState.PENDING );
    } else {
      vm.setState( newState );
    }
    try {
      vm.store();
    } catch ( final Exception ex1 ) {
      LOG.error( ex1, ex1 );
    }
    return action;
  }

  private static void restoreServiceTag( final String serviceTag, final String instanceId ) {
    try ( final TransactionResource tx = Entities.transactionFor( VmInstance.class ) ) {
      final VmInstance vm = VmInstances.lookup( instanceId );
      VmInstances.setServiceTag( vm, serviceTag );
      tx.commit( );
    } catch ( final Exception e ) {
      LOG.error( "Error restoring service tag ("+serviceTag+") for instance ("+instanceId+")", e );
    }
  }

  /**
   * Caller must have session for given vm
   */
  private static Callable<Boolean> tryCleanUpRunnable( final VmInstance vm ) {
    return cleanUpRunnable( vm, null, new Predicate<VmInstance>( ) {
      @Override
      public boolean apply( final VmInstance vmInstance ) {
        VmInstances.tryCleanUp( vmInstance );
        return true;
      }
    } );
  }

  /**
   * Caller must have session for given vm
   */
  private static Callable<Boolean> cleanUpRunnable( final VmInstance vm ) {
    return cleanUpRunnable( vm, null );
  }

  /**
   * Caller must have session for given vm
   */
  private static Callable<Boolean> cleanUpRunnable( final VmInstance vm, @Nullable final String reason ) {
    return cleanUpRunnable( vm, reason, new Predicate<VmInstance>( ) {
      @Override
      public boolean apply( final VmInstance vmInstance ) {
        VmInstances.cleanUp( vmInstance );
        return true;
      }
    } );
  }

  /**
   * Caller must have session for given vm
   */
  private static Callable<Boolean> cleanUpRunnable( final VmInstance vm,
                                                    @Nullable final String reason,
                                                    final Predicate<VmInstance> cleaner ) {
    Logs.extreme( ).info( "Preparing to clean-up instance: " + vm.getInstanceId(),
        Exceptions.filterStackTrace( new RuntimeException( ) ) );
    final String instanceId = vm.getInstanceId();
    return new Threads.EucaCallable<Boolean>( ) {
      @Override
      public Boolean call( ) {
        cleaner.apply( vm );
        if ( ( reason != null ) && !vm.getRuntimeState( ).getReasonDetails( ).contains( reason ) ) {
          vm.getRuntimeState( ).addReasonDetail( reason );
        }
        return Boolean.TRUE;
      }

      @Override
      public String getCorrelationId() {
        final BaseMessage req = MessageContexts.lookupLast(instanceId ,
            Sets.<Class>newHashSet(
                TerminateInstancesType.class,
                StopInstancesType.class
            ));
        return req == null ? null : req.getCorrelationId();
      }
    };
  }

  private enum ValidateVmInfo implements Predicate<VmInfo> {
    INSTANCE;

    @Override
    public boolean apply( VmInfo arg0 ) {
      if ( arg0.getInstanceType( ).getName( ) == null ) {
        LOG.warn( "Instance " + arg0.getInstanceId( ) + " reported no instance type: " + arg0.getInstanceType( ) );
      }
      if ( arg0.getInstanceType( ).getVirtualBootRecord( ).isEmpty( ) ) {
        LOG.warn( "Instance " + arg0.getInstanceId( ) + " reported no vbr entries: " + arg0.getInstanceType( ).getVirtualBootRecord( ) );
        return false;
      }
      try {
        Topology.lookup( ClusterController.class, Clusters.lookup( arg0.getPlacement( ) ).lookupPartition( ) );
      } catch ( NoSuchElementException ex ) {
        return false;//GRZE:ARG: skip restoring while cluster is enabling since Builder.placement() depends on a running cluster...
      }
      return true;
    }

  }

  public enum Create implements Function<VmInstanceToken, VmInstance> {
    INSTANCE;

    /**
     * @see Predicate#apply(Object)
     */
    @Override
    public VmInstance apply( final VmInstanceToken token ) {
      final EntityTransaction db = Entities.get( VmInstance.class );
      try {
        // remove existing persistent terminated instance.
        try {
          Entities.delete( Entities.uniqueResult( VmInstance.withUuid( token.getInstanceUuid( ) ) ) );
          Entities.flush( VmInstance.class );
        } catch ( NoSuchElementException e ) {
          // OK, no persistent entity
        }

        final Allocations.Allocation allocInfo = token.getAllocationInfo( );
        final Builder builder = new Builder( );
        builder.onBuild( new Callback<VmInstance>() {
          @Override
          public void fire( final VmInstance input ) {
            final VmInstance persistedInstance = Entities.persist( input );
            final List<ResourceTag> instanceTags =
                TagHelper.tagsForResource( allocInfo.getRequest( ).getTagSpecification( ), PolicySpec.EC2_RESOURCE_INSTANCE );
            TagHelper.createOrUpdateTags( allocInfo.getOwnerFullName( ), persistedInstance, instanceTags );
          }
        } );
        VmInstanceLifecycleHelper.get().prepareVmInstance( token, builder );
        VmInstance vmInst = builder
            .owner( allocInfo.getOwnerFullName( ) )
            .withIds( token.getInstanceId(),
                token.getInstanceUuid(),
                allocInfo.getReservationId(),
                allocInfo.getClientToken(),
                allocInfo.getUniqueClientToken( token.getLaunchIndex( ) ) )
            .bootRecord( allocInfo.getBootSet( ),
                allocInfo.getUserData( ),
                allocInfo.getSshKeyPair( ),
                allocInfo.getVmType( ),
                allocInfo.getSubnet( ),
                allocInfo.isMonitoring(),
                allocInfo.getIamInstanceProfileArn(),
                allocInfo.getIamInstanceProfileId(),
                allocInfo.getIamRoleArn() )
            .placement( allocInfo.getPartition( ) )
            .networkGroups( allocInfo.getNetworkGroups() )
            .addressing( allocInfo.isUsePrivateAddressing() )
            .disableApiTermination( allocInfo.isDisableApiTermination() )
            .zombie( token.isZombie( ) )
            .expiresOn( allocInfo.getExpiration() )
            .build( token.getLaunchIndex( ) );
        Entities.flush( vmInst );
        db.commit( );
        token.setVmInstance( vmInst );
        return vmInst;
      } catch ( final ResourceAllocationException ex ) {
        Logs.extreme( ).error( ex, ex );
        throw Exceptions.toUndeclared( ex );
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        throw Exceptions.toUndeclared( new TransactionExecutionException( ex ) );
      } finally {
        if ( db.isActive() ) db.rollback();
      }
    }
  }

  public static class Builder {
    private VmId                vmId;
    private String              uuid;
    private VmBootRecord        vmBootRecord;
    private VmPlacement         vmPlacement;
    private List<NetworkGroup>  networkRulesGroups;
    private Boolean             usePrivateAddressing;
    private Boolean             zombie;
    private Boolean             disableApiTermination;
    private OwnerFullName       owner;
    private Date                expiration = new Date( 32503708800000l ); // 3000
    private List<Callback<VmInstance>> callbacks = Lists.newArrayList( );

    public Builder owner( final OwnerFullName owner ) {
      this.owner = owner;
      return this;
    }

    public Builder expiresOn( final Date expirationTime ) {
      if ( expirationTime != null ) {
        this.expiration = expirationTime;
      }
      return this;
    }

    public Builder networkGroups( final List<NetworkGroup> groups ) {
      this.networkRulesGroups = groups;
      return this;
    }

    public Builder addressing( final Boolean usePrivate ) {
      this.usePrivateAddressing = usePrivate;
      return this;
    }

    public Builder disableApiTermination( final Boolean disableApiTermination ) {
      this.disableApiTermination = disableApiTermination;
      return this;
    }

    public Builder zombie( final Boolean zombie ) {
      this.zombie = zombie;
      return this;
    }

    public Builder withIds( @Nonnull  final String instanceId,
                            @Nonnull  final String instanceUuid,
                            @Nonnull  final String reservationId,
                            @Nullable final String clientToken,
                            @Nullable final String uniqueClientToken ) {
      this.vmId = new VmId( reservationId, instanceId, clientToken, uniqueClientToken );
      this.uuid = instanceUuid;
      return this;
    }

    public Builder placement( final Partition partition ) {
      final ServiceConfiguration config = Topology.lookup( ClusterController.class, partition );
      this.vmPlacement = new VmPlacement( config.getName( ), config.getPartition( ) );
      return this;
    }

    public Builder bootRecord( final Emis.BootableSet bootSet,
                               final byte[] userData,
                               final SshKeyPair sshKeyPair,
                               final VmType vmType,
                               final Subnet subnet,
                               final boolean monitoring,
                               @Nullable final String iamInstanceProfileArn,
                               @Nullable final String iamInstanceProfileId,
                               @Nullable final String iamInstanceRoleArn ) {
      checkParam( "BootSet must not be null", bootSet, notNullValue( ) );
      ImageInfo machineImage = null;
      KernelImageInfo kernel = null;
      RamdiskImageInfo ramdisk = null;
      ImageMetadata.Architecture architecture = null;
      ImageMetadata.Platform platform = null;

      if ( bootSet.getMachine() instanceof ImageInfo ) {
        machineImage = ( ImageInfo ) bootSet.getMachine( );
      }
      if ( bootSet.hasKernel( ) ) {
        kernel = bootSet.getKernel( );
      }
      if ( bootSet.hasRamdisk( ) ) {
        ramdisk = bootSet.getRamdisk( );
      }
      architecture = (bootSet.getMachine() != null) ? bootSet.getMachine().getArchitecture() : null;
      platform = bootSet.getMachine( ).getPlatform( );

      this.vmBootRecord = new VmBootRecord( machineImage, kernel, ramdisk, architecture, platform, userData, sshKeyPair,
          vmType, subnet, monitoring, iamInstanceProfileArn, iamInstanceProfileId, iamInstanceRoleArn );
      return this;
    }

    public Builder onBuild( final Callback<VmInstance> callback ) {
      callbacks.add( callback );
      return this;
    }

    public VmInstance build( final Integer launchIndex ) throws ResourceAllocationException {
      VmInstance instance = new VmInstance( this.owner, this.vmId, this.vmBootRecord, new VmLaunchRecord( launchIndex, new Date( ) ), this.vmPlacement,
          this.networkRulesGroups, this.usePrivateAddressing, this.disableApiTermination, this.expiration );
      instance.setNaturalId( uuid );
      if ( Boolean.TRUE.equals( this.zombie ) ) {
        instance.getRuntimeState( ).zombie( );
      }
      for ( final Callback<VmInstance> callback : callbacks ) {
        callback.fire( instance );
      }
      return instance;
    }
  }

  public enum RestoreHandler implements Predicate<VmInfo> {
    /**
     * Restores non-VPC instances. No longer supported, now the same as RestoreFailed.
     */
    Restore {
      @Override
      public boolean apply( final VmInfo input ) {
        return RestoreFailed.apply( input );
      }
    },
    /**
     * Restores instances without network resources.
     */
    RestoreFailed {
      @Override
      public boolean apply( final VmInfo input ) {
        final VmState inputState = VmState.Mapper.get( input.getStateName( ) );
        if ( !VmStateSet.RUN.contains( inputState ) ) {
          return false;
        } else if ( !ValidateVmInfo.INSTANCE.apply( input ) ) {
          return false;
        } else {
          final UserFullName userFullName =
              UserFullName.getInstanceForAccount( input.getAccountId( ), input.getOwnerId( ) );
          Allocations.Allocation allocation = null;
          try {
            final String imageId = RestoreHandler.restoreImage( input );
            final String kernelId = RestoreHandler.restoreKernel( input );
            final String ramdiskId = RestoreHandler.restoreRamdisk( input );

            allocation = Allocations.restore(
                input,
                RestoreHandler.restoreLaunchIndex( input ),
                RestoreHandler.restoreVmType( input ),
                RestoreHandler.restoreBootSet( input, imageId, kernelId, ramdiskId ),
                RestoreHandler.restorePartition( input ),
                RestoreHandler.restoreSshKeyPair( input, userFullName ),
                RestoreHandler.restoreUserData( input ),
                userFullName );

            allocation.commit( );

            restoreServiceTag( input.getServiceTag( ), input.getInstanceId( ) );

            return true;
          } catch ( final Exception ex ) {
            if ( allocation != null ) allocation.abort( );
            LOG.error( "Failed to restore instance " + input.getInstanceId( ) + " because of: " + ex.getMessage( ), /*building ? null :*/ ex );
            Logs.extreme( ).error( ex, ex );
            return false;
          }
        }
      }
    },
    /**
     * Unconditionally terminate instances (ebs instances can be restarted)
     */
    Terminate {
      @Override
      public boolean apply( final VmInfo input ) {
        final VmState inputState = VmState.Mapper.get( input.getStateName() );
        if ( VmStateSet.RUN.contains( inputState ) ) {
          try {
            final String partition = Clusters.lookup( input.getPlacement( ) ).getPartition( );
            LOG.info( "Requesting termination for instance " + input.getInstanceId( ) + " in zone " + partition );
            sendTerminate( input.getInstanceId( ), partition );
          } catch ( final NoSuchElementException e ) {
            LOG.info( "Partition lookup failed, skipping terminate attempt for cluster: " + input.getPlacement( ) + ", instance " + input.getInstanceId() );
          }
        }
        return true;
      }
    },
    /**
     * Terminate instance if metadata is available that shows the instance is not expected to be running.
     */
    TerminateDone {
      @Override
      public boolean apply( final VmInfo input ) {
        try {
          final VmInstance instance = lookupAny( input.getInstanceId( ) );
          final VmInstance.Reason reason = instance.getRuntimeState( ).reason( );
          if ( instance.getNaturalId( ).equals( input.getUuid( ) ) &&
              VmStateSet.NOT_RUNNING.apply( instance ) &&
              reason != null && reason.user( ) ) {
            return Terminate.apply( input );
          }
        } catch ( final NoSuchElementException e ) {
          // unknown, so do not terminate
        } catch ( final Exception e ) {
          // error, do not terminate
          LOG.error( "Error handing unknown instance: " + input.getInstanceId( ), e );
        }
        return false;
      }
    },
    ;

    public static Iterable<Optional<RestoreHandler>> parseList( final String handlers ) {
      final List<Optional<RestoreHandler>> handlerList = Lists.newArrayList( );
      for ( final String handler : Splitter.on( ',' ).omitEmptyStrings( ).trimResults( ).split( handlers ) ) {
        final String handerEnum = CaseFormat.LOWER_HYPHEN.to( CaseFormat.UPPER_CAMEL, handler );
        handlerList.add( Enums.getIfPresent( RestoreHandler.class, handerEnum ) );
      }
      return handlerList;
    }

    private static Function<String, NetworkGroup> transformNetworkNames( final UserFullName userFullName ) {
      return new Function<String, NetworkGroup>( ) {

        @Override
        public NetworkGroup apply( final String arg0 ) {

          final EntityTransaction db = Entities.get( NetworkGroup.class );
          try {
            SimpleExpression naturalId = Restrictions.like( "naturalId", arg0.replace( userFullName.getAccountNumber( ) + "-", "" ) + "%" );
            NetworkGroup result = ( NetworkGroup ) Entities.createCriteria( NetworkGroup.class )
                .add( naturalId )
                .uniqueResult( );
            if ( result == null ) {
              SimpleExpression displayName = Restrictions.like( "displayName", arg0.replace( userFullName.getAccountNumber( ) + "-", "" ) + "%" );
              result = ( NetworkGroup ) Entities.createCriteria( NetworkGroup.class )
                  .add( displayName )
                  .uniqueResult( );
            }
            db.commit( );
            return result;
          } catch ( Exception ex ) {
            Logs.extreme( ).error( ex, ex );
            throw Exceptions.toUndeclared( ex );
          } finally {
            if ( db.isActive() ) db.rollback();
          }
        }
      };
    }

    private static byte[] restoreUserData( final VmInfo input ) {
      byte[] userData = new byte[0];
      if ( com.google.common.base.Strings.emptyToNull( input.getUserData() ) != null ) try {
        userData = Base64.decode( input.getUserData() );
      } catch ( final Exception ex ) {
        LOG.error("Failed to restore user data for : " + input.getInstanceId( ) + " because of: " + ex.getMessage( ) );
      }
      return userData;
    }

    private static SshKeyPair restoreSshKeyPair( final VmInfo input, final UserFullName userFullName ) {
      String keyValue = input.getKeyValue( );
      if ( keyValue == null || keyValue.indexOf( "@" ) == -1 ) {
        return KeyPairs.noKey();
      } else {
        String keyName = keyValue.replaceAll( ".*@eucalyptus\\.", "" );
        return SshKeyPair.withPublicKey( null, keyName, keyValue );
      }
    }

    private static int restoreLaunchIndex( final VmInfo input ) {
      int launchIndex = 1;
      try {
        launchIndex = Integer.parseInt( input.getLaunchIndex( ) );
      } catch ( final Exception ex1 ) {
        LOG.debug("Failed to get LaunchIndex setting it to '1' for: " + input.getInstanceId( ) + " because of: " + ex1.getMessage( ) );
        launchIndex = 1;
      }
      return launchIndex;
    }

    @Nonnull
    private static Emis.BootableSet restoreBootSet( @Nonnull  final VmInfo input,
                                                    @Nullable final String imageId,
                                                    @Nullable final String kernelId,
                                                    @Nullable final String ramdiskId ) throws MetadataException {
      if ( imageId == null ) {
        throw new MetadataException( "Missing image id for boot set restoration" );
      }

      Emis.BootableSet bootSet;
      try {
        bootSet = Emis.recreateBootableSet( imageId, kernelId, ramdiskId );
      } catch ( final NoSuchMetadataException e ) {
        LOG.error( "Using transient bootset in place of imageId " + imageId
            + ", kernelId " + kernelId
            + ", ramdiskId " + ramdiskId
            + " for " + input.getInstanceId( )
            + " because of: " + e.getMessage( ) );
        ImageMetadata.Platform platform;
        try {
          platform = ImageMetadata.Platform.valueOf( com.google.common.base.Strings.nullToEmpty( input.getPlatform() ) );
        } catch ( final IllegalArgumentException e2 ) {
          platform = ImageMetadata.Platform.linux;
        }
        bootSet = Emis.unavailableBootableSet( platform );
      }  catch ( final Exception ex ) {
        LOG.error( "Failed to recreate bootset with imageId " + imageId
            + ", kernelId " + kernelId
            + ", ramdiskId " + ramdiskId
            + " for " + input.getInstanceId( )
            + " because of: " + ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
        if ( ex instanceof MetadataException ) {
          throw (MetadataException) ex;
        } else {
          throw Exceptions.toUndeclared( ex );
        }
      }

      return bootSet;
    }

    private static String restoreRamdisk( final VmInfo input ) {
      String ramdiskId = null;
      try {
        ramdiskId = input.getInstanceType( ).lookupRamdisk( ).getId( );
      } catch ( final NoSuchElementException ex ) {
        LOG.debug( "No ramdiskId " + input.getRamdiskId( )
            + " for: "
            + input.getInstanceId( )
            + " because vbr does not contain a ramdisk: "
            + input.getInstanceType( ).getVirtualBootRecord( ) );
        Logs.extreme( ).error( ex, ex );
      } catch ( final Exception ex ) {
        LOG.error( "Failed to lookup ramdiskId " + input.getRamdiskId( ) + " for: " + input.getInstanceId( ) + " because of: " + ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
      }
      return ramdiskId;
    }

    private static String restoreKernel( final VmInfo input ) {
      String kernelId = null;
      try {
        kernelId = input.getInstanceType( ).lookupKernel( ).getId( );
      } catch ( final NoSuchElementException ex ) {
        LOG.debug( "No kernelId " + input.getKernelId( )
            + " for: "
            + input.getInstanceId( )
            + " because vbr does not contain a kernel: "
            + input.getInstanceType( ).getVirtualBootRecord( ) );
        Logs.extreme( ).error( ex, ex );
      } catch ( final Exception ex ) {
        LOG.error( "Failed to lookup kernelId " + input.getKernelId( ) + " for: " + input.getInstanceId( ) + " because of: " + ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
      }
      return kernelId;
    }

    private static String restoreImage( final VmInfo input ) {
      String imageId = null;
      try {
        imageId = input.getInstanceType( ).lookupRoot( ).getId( );
      } catch ( final Exception ex2 ) {
        LOG.error( "Failed to lookup imageId " + input.getImageId( ) + " for: " + input.getInstanceId( ) + " because of: " + ex2.getMessage( ) );
        Logs.extreme( ).error( ex2, ex2 );
      }
      return imageId;
    }

    private static Partition restorePartition( final VmInfo input ) {
      Partition partition = null;
      try {
        partition = Partitions.lookupByName( input.getPlacement() );
      } catch ( final Exception ex2 ) {
        try {
          partition = Partitions.lookupByName( Clusters.lookup( input.getPlacement( ) ).getPartition( ) );
        } catch ( final Exception ex ) {
          LOG.error( "Failed to lookup partition " + input.getPlacement( ) + " for: " + input.getInstanceId( ) + " because of: " + ex.getMessage( ) );
          Logs.extreme( ).error( ex, ex );
        }
      }
      return partition;
    }

    private static VmType restoreVmType( final VmInfo input ) {
      VmType vmType = null;
      try {
        vmType = VmTypes.lookup( input.getInstanceType().getName() );
      } catch ( final Exception ex ) {
        LOG.error( "Failed to lookup vm type " + input.getInstanceType( ).getName( ) + " for: " + input.getInstanceId( ) + " because of: " + ex.getMessage( ) );
        Logs.extreme( ).error( ex, ex );
      }
      return vmType;
    }

  }

  public static class VmInstanceExpiredStateEventListener implements EventListener<ClockTick> {

    public static void register( ) {
      Listeners.register( ClockTick.class, new VmInstanceExpiredStateEventListener( ) );
    }

    private static Criterion criterion( final Timeout timeout ) {
      return Restrictions.and(
          Restrictions.lt( "lastUpdateTimestamp", new Date( System.currentTimeMillis( ) - timeout.getMilliseconds( ) ) ),
          VmInstance.criterion( timeout.states.toArray( new VmState[ timeout.states.size( ) ] ) )
      );
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Topology.isEnabledLocally( Eucalyptus.class ) &&
          Hosts.isCoordinator( ) &&
          Bootstrap.isOperational( ) &&
          !Databases.isVolatile( ) ) {
        final List<VmInstance> instances = Lists.newArrayList( );
        try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( VmInstance.class ) ) {
          instances.addAll( list( null,
              Restrictions.or(
                  criterion( Timeout.BURIED ),
                  criterion( Timeout.TERMINATED ),
                  Restrictions.and( criterion( Timeout.STOPPING ), VmInstance.nullNodeCriterion( ) ),
                  Restrictions.and( criterion( Timeout.SHUTTING_DOWN ), VmInstance.nullNodeCriterion( ) )
              ),
              Collections.<String, String>emptyMap( ),
              Predicates.or(
                  Timeout.BURIED,
                  Timeout.TERMINATED,
                  Predicates.and( Timeout.STOPPING, Predicates.compose( Predicates.isNull( ), toServiceTag( ) ) ),
                  Predicates.and( Timeout.SHUTTING_DOWN, Predicates.compose( Predicates.isNull( ), toServiceTag( ) ) )
              ) ) );
        }
        for ( final VmInstance instance : instances ) try {
          switch ( instance.getState( ) ) {
            case STOPPING:
              VmInstances.stopped( instance );
              break;
            case SHUTTING_DOWN:
              VmInstances.terminated( instance );
              break;
            case TERMINATED:
              VmInstances.buried( instance );
              break;
            case BURIED:
              VmInstances.delete( instance );
              break;
          }
        } catch ( final Exception ex ) {
          LOG.error( ex );
          Logs.extreme( ).error( ex, ex );
        }
      }
    }
  }

  enum InstanceStatusUpdate implements Function<VmInstance, VmInstance> {
    REACHABLE {
      @Override
      public VmInstance apply( final VmInstance v ) {
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          if ( VmState.RUNNING.apply( vm ) ) {
            vm.getRuntimeState( ).reachable( );
          }
          return vm;
        } catch ( final Exception ex ) {
          Logs.extreme().trace( ex, ex );
          throw new NoSuchElementException( "Failed to lookup instance: " + v );
        }
      }
    },
    UNREACHABLE {
      @Override
      public VmInstance apply( final VmInstance v ) {
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, v.getInstanceId( ) ) );
          if ( VmState.RUNNING.apply( vm ) ) {
            final VmRuntimeState runtimeState = vm.getRuntimeState( );
            final Date unreachableSince = runtimeState.getUnreachableTimestamp();
            if ( unreachableSince == null ) {
              runtimeState.setUnreachableTimestamp( new Date() );
            } else if ( unreachableSince.getTime( ) + TimeUnit.MINUTES.toMillis( VmInstances.INSTANCE_REACHABILITY_TIMEOUT ) <
                System.currentTimeMillis( ) ) {
              runtimeState.setInstanceStatus( VmRuntimeState.InstanceStatus.Impaired );
              runtimeState.setReachabilityStatus( VmRuntimeState.ReachabilityStatus.Failed );
            }
          }
          return vm;
        } catch ( final Exception ex ) {
          Logs.extreme().trace( ex, ex );
          throw new NoSuchElementException( "Failed to lookup instance: " + v );
        }
      }
    },
  }

  private enum InstanceInitialize implements Predicate<VmInstance> {
    INSTANCE;

    @Override
    public boolean apply( final VmInstance input ) {
      Entities.initialize( input.getNetworkGroups( ) );
      Entities.initialize( input.getNetworkGroupIds( ) );
      Entities.initialize( input.getTags( ) );
      input.getRuntimeState( ).getDisplayReason( ); // Initializes reason details
      Entities.initialize( input.getBootRecord( ).getPersistentVolumes( ) );
      Entities.initialize( input.getTransientVolumeState( ).getAttachments( ) );
      return true;
    }
  }

  public static Function<VmInstance,VmBundleTask> bundleTask() {
    return VmInstanceToVmBundleTask.INSTANCE;
  }

  private static <T> Set<T> blockDeviceSet( final VmInstance instance,
                                            final Function<? super VmVolumeAttachment,T> transform ) {
    return Sets.newHashSet( Iterables.transform(
        Iterables.concat(
            instance.getBootRecord( ).getPersistentVolumes( ),
            instance.getTransientVolumeState( ).getAttachments( ) ),
        transform ) );
  }

  private static <T> Set<T> networkGroupSet( final VmInstance instance,
                                             final Function<? super NetworkGroup,T> transform ) {
    return instance.getNetworkGroups() != null ?
        Sets.newHashSet( Iterables.transform( instance.getNetworkGroups(), transform ) ) :
        Collections.<T>emptySet();
  }

  private static <T> Set<T> networkInterfaceSet( final VmInstance instance,
                                                 final Function<? super NetworkInterface,T> transform ) {
    return instance.getNetworkInterfaces() != null ?
        Sets.newHashSet( Iterables.transform( instance.getNetworkInterfaces(), transform ) ) :
        Collections.<T>emptySet( );
  }

  private static <T> Set<T> networkInterfaceSetSet( final VmInstance instance,
                                                    final Function<? super NetworkInterface,Set<T>> transform ) {
    return instance.getNetworkInterfaces() != null ?
        Sets.newHashSet( Iterables.concat( Iterables.transform( instance.getNetworkInterfaces(), transform ) ) ) :
        Collections.<T>emptySet( );
  }

  public static class VmInstanceFilterSupport extends FilterSupport<VmInstance> {
    public VmInstanceFilterSupport() {
      super( builderFor( VmInstance.class )
          .withTagFiltering( VmInstanceTag.class, "instance" )
          .withStringProperty( "architecture", VmInstanceFilterFunctions.ARCHITECTURE )
          .withStringProperty( "availability-zone", VmInstanceFilterFunctions.AVAILABILITY_ZONE )
          .withDateSetProperty( "block-device-mapping.attach-time", VmInstanceDateSetFilterFunctions.BLOCK_DEVICE_MAPPING_ATTACH_TIME )
          .withBooleanSetProperty( "block-device-mapping.delete-on-termination", VmInstanceBooleanSetFilterFunctions.BLOCK_DEVICE_MAPPING_DELETE_ON_TERMINATE )
          .withStringSetProperty( "block-device-mapping.device-name", VmInstanceStringSetFilterFunctions.BLOCK_DEVICE_MAPPING_DEVICE_NAME )
          .withStringSetProperty( "block-device-mapping.status", VmInstanceStringSetFilterFunctions.BLOCK_DEVICE_MAPPING_STATUS )
          .withStringSetProperty( "block-device-mapping.volume-id", VmInstanceStringSetFilterFunctions.BLOCK_DEVICE_MAPPING_VOLUME_ID )
          .withStringProperty( "client-token", VmInstance.clientToken( ) )
          .withStringProperty( "dns-name", VmInstanceFilterFunctions.DNS_NAME )
          .withStringSetProperty( "group-id", VmInstanceStringSetFilterFunctions.GROUP_ID )
          .withStringSetProperty( "group-name", VmInstanceStringSetFilterFunctions.GROUP_NAME )
          .withStringProperty( "image-id", VmInstanceFilterFunctions.IMAGE_ID )
          .withStringProperty( "iam-instance-profile.arn", VmInstanceFilterFunctions.INSTANCE_PROFILE_ARN )
          .withStringProperty( "instance-id", CloudMetadatas.toDisplayName() )
          .withConstantProperty( "instance-lifecycle", "" )
          .withIntegerProperty( "instance-state-code", VmInstanceIntegerFilterFunctions.INSTANCE_STATE_CODE )
          .withStringProperty( "instance-state-name", VmInstanceFilterFunctions.INSTANCE_STATE_NAME )
          .withStringProperty( "instance-type", VmInstanceFilterFunctions.INSTANCE_TYPE )
          .withStringSetProperty( "instance.group-id", VmInstanceStringSetFilterFunctions.GROUP_ID )
          .withStringSetProperty( "instance.group-name", VmInstanceStringSetFilterFunctions.GROUP_NAME )
          .withStringProperty( "ip-address", VmInstanceFilterFunctions.IP_ADDRESS )
          .withStringProperty( "kernel-id", VmInstanceFilterFunctions.KERNEL_ID )
          .withStringProperty( "key-name", VmInstanceFilterFunctions.KEY_NAME )
          .withStringProperty( "launch-index", VmInstanceFilterFunctions.LAUNCH_INDEX )
          .withDateProperty( "launch-time", VmInstanceDateFilterFunctions.LAUNCH_TIME )
          .withStringProperty( "monitoring-state", VmInstanceFilterFunctions.MONITORING_STATE )
          .withStringProperty( "owner-id", VmInstanceFilterFunctions.OWNER_ID )
          .withUnsupportedProperty( "placement-group-name" )
          .withStringProperty( "platform", VmInstanceFilterFunctions.PLATFORM )
          .withStringProperty( "private-dns-name", VmInstanceFilterFunctions.PRIVATE_DNS_NAME )
          .withStringProperty( "private-ip-address", VmInstanceFilterFunctions.PRIVATE_IP_ADDRESS )
          .withUnsupportedProperty( "product-code" )
          .withUnsupportedProperty( "product-code.type" )
          .withStringProperty( "ramdisk-id", VmInstanceFilterFunctions.RAMDISK_ID )
          .withStringProperty( "reason", VmInstanceFilterFunctions.REASON )
          .withUnsupportedProperty( "requester-id" )
          .withStringProperty( "reservation-id", VmInstanceFilterFunctions.RESERVATION_ID )
          .withStringProperty( "root-device-name", VmInstanceFilterFunctions.ROOT_DEVICE_NAME )
          .withStringProperty( "root-device-type", VmInstanceFilterFunctions.ROOT_DEVICE_TYPE )
          .withUnsupportedProperty( "source-dest-check" )
          .withUnsupportedProperty( "spot-instance-request-id" )
          .withUnsupportedProperty( "state-reason-code" )
          .withUnsupportedProperty( "state-reason-message" )
          .withStringProperty( "subnet-id", VmInstanceFilterFunctions.SUBNET_ID )
          .withConstantProperty( "tenancy", "default" )
          .withStringProperty( "virtualization-type", VmInstanceFilterFunctions.VIRTUALIZATION_TYPE )
          .withStringProperty( "vpc-id", VmInstanceFilterFunctions.VPC_ID )
          .withUnsupportedProperty( "hypervisor" )
          .withStringSetProperty( "network-interface.description", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_DESCRIPTION )
          .withStringSetProperty( "network-interface.subnet-id", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_SUBNET_ID )
          .withStringSetProperty( "network-interface.vpc-id", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_VPC_ID )
          .withStringSetProperty( "network-interface.network-interface.id", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_ID )
          .withStringSetProperty( "network-interface.owner-id", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_OWNER_ID )
          .withStringSetProperty( "network-interface.availability-zone", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_AVAILABILITY_ZONE )
          .withUnsupportedProperty( "network-interface.requester-id" )
          .withUnsupportedProperty( "network-interface.requester-managed" )
          .withStringSetProperty( "network-interface.status", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_STATUS )
          .withStringSetProperty( "network-interface.mac-address", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_MAC_ADDRESS )
          .withStringSetProperty( "network-interface-private-dns-name", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_PRIVATE_DNS_NAME )
          .withBooleanSetProperty( "network-interface.source-destination-check", VmInstanceBooleanSetFilterFunctions.NETWORK_INTERFACE_SOURCE_DEST_CHECK )
          .withStringSetProperty( "network-interface.group-id", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_GROUP_ID )
          .withStringSetProperty( "network-interface.group-name", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_GROUP_NAME )
          .withStringSetProperty( "network-interface.addresses.private-ip-address", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_PRIVATE_IP )
          .withBooleanSetProperty( "network-interface.addresses.primary", VmInstanceBooleanSetFilterFunctions.NETWORK_INTERFACE_ADDRESSES_PRIMARY )
          .withStringSetProperty( "network-interface.addresses.association.public-ip", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_ASSOCIATION_PUBLIC_IP )
          .withStringSetProperty( "network-interface.addresses.association.ip-owner-id", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_ASSOCIATION_IP_OWNER_ID )
          .withStringSetProperty( "network-interface.attachment.attachment-id", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_ATTACHMENT_ID )
          .withStringSetProperty( "network-interface.attachment.instance-id", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_ATTACHMENT_INSTANCE_ID )
          .withStringSetProperty( "network-interface.attachment.instance-owner-id", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_ATTACHMENT_INSTANCE_OWNER_ID )
          .withIntegerSetProperty( "network-interface.attachment.device-index", VmInstanceIntegerSetFilterFunctions.NETWORK_INTERFACE_ATTACHMENT_DEVICE_INDEX )
          .withStringSetProperty( "network-interface.attachment.status", VmInstanceStringSetFilterFunctions.NETWORK_INTERFACE_ATTACHMENT_STATUS )
          .withDateSetProperty( "network-interface.attachment.attach-time", VmInstanceDateSetFilterFunctions.NETWORK_INTERFACE_ATTACHMENT_ATTACH_TIME )
          .withBooleanSetProperty( "network-interface.attachment.delete-on-termination", VmInstanceBooleanSetFilterFunctions.NETWORK_INTERFACE_ATTACHMENT_DELETE_ON_TERMINATION )
          .withStringSetProperty( "association.public-ip", VmInstanceStringSetFilterFunctions.ASSOCIATION_PUBLIC_IP )
          .withStringSetProperty( "association.ip-owner-id", VmInstanceStringSetFilterFunctions.ASSOCIATION_IP_OWNER_ID )
          .withStringSetProperty( "association.allocation-id", VmInstanceStringSetFilterFunctions.ASSOCIATION_ALLOCATION_ID )
          .withStringSetProperty( "association.association-id", VmInstanceStringSetFilterFunctions.ASSOCIATION_ID )
          .withPersistenceAlias( "bootRecord.machineImage", "image" )
          .withPersistenceAlias( "networkGroups", "networkGroups" )
          .withPersistenceAlias( "bootRecord.vmType", "vmType" )
          .withPersistenceAlias( "networkConfig.networkInterfaces", "networkInterfaces" )
          .withPersistenceAlias( "networkInterfaces.vpc", "vpc" )
          .withPersistenceAlias( "networkInterfaces.subnet", "subnet" )
          .withPersistenceAlias( "networkInterfaces.networkGroups", "networkInterfaceNetworkGroups" )
          .withPersistenceFilter( "architecture", "image.architecture", Sets.newHashSet( "bootRecord.machineImage" ), FUtils.valueOfFunction( ImageMetadata.Architecture.class ) )
          .withPersistenceFilter( "availability-zone", "placement.partitionName", Collections.<String>emptySet() )
          .withPersistenceFilter( "client-token", "vmId.clientToken", Collections.<String>emptySet() )
          .withPersistenceFilter( "group-id", "networkGroups.groupId" )
          .withPersistenceFilter( "group-name", "networkGroups.displayName" )
          .withPersistenceFilter( "iam-instance-profile.arn", "bootRecord.iamInstanceProfileArn", Collections.<String>emptySet() )
          .withPersistenceFilter( "image-id", "image.displayName", Sets.newHashSet( "bootRecord.machineImage" ) )
          .withPersistenceFilter( "instance-id", "displayName" )
          .withPersistenceFilter( "instance-type", "vmType.name", Sets.newHashSet( "bootRecord.vmType" ) )
          .withPersistenceFilter( "instance.group-id", "networkGroups.groupId" )
          .withPersistenceFilter( "instance.group-name", "networkGroups.displayName" )
          .withPersistenceFilter( "kernel-id", "image.kernelId", Sets.newHashSet( "bootRecord.machineImage" ) )
          .withPersistenceFilter( "launch-index", "launchRecord.launchIndex", Collections.<String>emptySet(), PersistenceFilter.Type.Integer )
          .withPersistenceFilter( "launch-time", "launchRecord.launchTime", Collections.<String>emptySet(), PersistenceFilter.Type.Date )
          .withPersistenceFilter( "owner-id", "ownerAccountNumber" )
          .withPersistenceFilter( "ramdisk-id", "image.ramdiskId", Sets.newHashSet( "bootRecord.machineImage" ) )
          .withPersistenceFilter( "reservation-id", "vmId.reservationId", Collections.<String>emptySet() )
          .withPersistenceFilter( "subnet-id", "bootRecord.subnetId", Collections.<String>emptySet() )
          .withPersistenceFilter( "virtualization-type", "bootRecord.virtType", Collections.<String>emptySet(), ImageMetadata.VirtualizationType.fromString() )
          .withPersistenceFilter( "vpc-id", "bootRecord.vpcId", Collections.<String>emptySet() )
          .withPersistenceFilter( "network-interface.description", "networkInterfaces.description", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.subnet-id", "subnet.displayName", Sets.newHashSet( "networkConfig.networkInterfaces", "networkInterfaces.subnet" ) )
          .withPersistenceFilter( "network-interface.vpc-id", "vpc.displayName", Sets.newHashSet( "networkConfig.networkInterfaces", "networkInterfaces.vpc" ) )
          .withPersistenceFilter( "network-interface.network-interface.id", "networkInterfaces.displayName", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.owner-id", "networkInterfaces.ownerAccountNumber", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.availability-zone", "networkInterfaces.availabilityZone", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.status", "networkInterfaces.state", Sets.newHashSet( "networkConfig.networkInterfaces" ), FUtils.valueOfFunction( NetworkInterface.State.class ) )
          .withPersistenceFilter( "network-interface.mac-address", "networkInterfaces.macAddress", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface-private-dns-name", "networkInterfaces.privateDnsName", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.source-destination-check", "networkInterfaces.sourceDestCheck", Sets.newHashSet( "networkConfig.networkInterfaces" ), PersistenceFilter.Type.Boolean )
          .withPersistenceFilter( "network-interface.group-id", "networkInterfaceNetworkGroups.groupId", Sets.newHashSet( "networkConfig.networkInterfaces", "networkInterfaces.networkGroups" ) )
          .withPersistenceFilter( "network-interface.group-name", "networkInterfaceNetworkGroups.displayName", Sets.newHashSet( "networkConfig.networkInterfaces", "networkInterfaces.networkGroups" )  )
          .withPersistenceFilter( "network-interface.addresses.private-ip-address", "networkInterfaces.privateIpAddress", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.addresses.association.public-ip", "networkInterfaces.association.publicIp", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.addresses.association.ip-owner-id", "networkInterfaces.association.ipOwnerId", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.attachment.attachment-id", "networkInterfaces.attachment.attachmentId", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.attachment.instance-id", "networkInterfaces.attachment.instanceId", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.attachment.instance-owner-id", "networkInterfaces.attachment.instanceOwnerId", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "network-interface.attachment.device-index", "networkInterfaces.attachment.deviceIndex", Sets.newHashSet( "networkConfig.networkInterfaces" ), PersistenceFilter.Type.Integer )
          .withPersistenceFilter( "network-interface.attachment.status", "networkInterfaces.attachment.status", Sets.newHashSet( "networkConfig.networkInterfaces" ), FUtils.valueOfFunction( NetworkInterfaceAttachment.Status.class ) )
          .withPersistenceFilter( "network-interface.attachment.attach-time", "networkInterfaces.attachment.attachTime", Sets.newHashSet( "networkConfig.networkInterfaces" ), PersistenceFilter.Type.Date )
          .withPersistenceFilter( "network-interface.attachment.delete-on-termination", "networkInterfaces.attachment.deleteOnTerminate", Sets.newHashSet( "networkConfig.networkInterfaces" ), PersistenceFilter.Type.Boolean )
          .withPersistenceFilter( "association.public-ip", "networkInterfaces.association.publicIp", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "association.ip-owner-id", "networkInterfaces.association.ipOwnerId", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "association.allocation-id", "networkInterfaces.association.allocationId", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
          .withPersistenceFilter( "association.association-id", "networkInterfaces.association.associationId", Sets.newHashSet( "networkConfig.networkInterfaces" ) )
      );
    }
  }

  public static class VmInstanceStatusFilterSupport extends FilterSupport<VmInstance> {
    public VmInstanceStatusFilterSupport() {
      super( qualifierBuilderFor( VmInstance.class, "status" )
              .withStringProperty( "availability-zone", VmInstanceFilterFunctions.AVAILABILITY_ZONE )
              .withStringSetProperty( "event.code", VmInstanceStringSetFilterFunctions.EVENT_CODE )
              .withStringSetProperty( "event.description", VmInstanceStringSetFilterFunctions.EVENT_DESCRIPTION )
              .withDateSetProperty( "event.not-after", VmInstanceDateSetFilterFunctions.EVENT_NOT_AFTER )
              .withDateSetProperty( "event.not-before", VmInstanceDateSetFilterFunctions.EVENT_NOT_BEFORE )
              .withInternalStringProperty( "instance-id", CloudMetadatas.toDisplayName() )
              .withIntegerProperty( "instance-state-code", VmInstanceIntegerFilterFunctions.INSTANCE_STATE_CODE )
              .withStringProperty( "instance-state-name", VmInstanceFilterFunctions.INSTANCE_STATE_NAME )
              .withStringProperty( "system-status.status", VmInstanceFilterFunctions.INSTANCE_STATUS )
              .withStringProperty( "system-status.reachability", VmInstanceFilterFunctions.INSTANCE_REACHABILITY_STATUS )
              .withStringProperty( "instance-status.status", VmInstanceFilterFunctions.INSTANCE_STATUS )
              .withStringProperty( "instance-status.reachability", VmInstanceFilterFunctions.INSTANCE_REACHABILITY_STATUS )
              .withPersistenceFilter( "availability-zone", "placement.partitionName", Collections.<String>emptySet() )
              .withPersistenceFilter( "instance-id", "displayName" )
              .withPersistenceFilter( "system-status.status", "runtimeState.instanceStatus", Collections.<String>emptySet( ), VmRuntimeState.InstanceStatus.fromString( ) )
              .withPersistenceFilter( "system-status.reachability", "runtimeState.reachabilityStatus", Collections.<String>emptySet( ), VmRuntimeState.ReachabilityStatus.fromString( ) )
              .withPersistenceFilter( "instance-status.status", "runtimeState.instanceStatus", Collections.<String>emptySet( ), VmRuntimeState.InstanceStatus.fromString( ) )
              .withPersistenceFilter( "instance-status.reachability", "runtimeState.reachabilityStatus", Collections.<String>emptySet( ), VmRuntimeState.ReachabilityStatus.fromString( ) )
      );
    }
  }

  public static class VmBundleTaskFilterSupport extends FilterSupport<VmBundleTask> {
    private enum ProgressToInteger implements Function<String,Integer> {
      INSTANCE {
        @Override
        public Integer apply( final String textValue ) {
          String cleanedValue = textValue;
          if ( cleanedValue.endsWith( "%" ) ) {
            cleanedValue = cleanedValue.substring( 0, cleanedValue.length() - 1 );
          }
          try {
            return java.lang.Integer.valueOf( cleanedValue );
          } catch ( NumberFormatException e ) {
            return null;
          }
        }
      }
    }

    public VmBundleTaskFilterSupport() {
      super( builderFor( VmBundleTask.class )
          .withStringProperty( "bundle-id", BundleFilterFunctions.BUNDLE_ID )
          .withStringProperty( "error-code", BundleFilterFunctions.ERROR_CODE )
          .withStringProperty( "error-message", BundleFilterFunctions.ERROR_MESSAGE )
          .withStringProperty( "instance-id", BundleFilterFunctions.INSTANCE_ID )
          .withStringProperty( "progress", BundleFilterFunctions.PROGRESS )
          .withStringProperty( "s3-bucket", BundleFilterFunctions.S3_BUCKET )
          .withStringProperty( "s3-prefix", BundleFilterFunctions.S3_PREFIX )
          .withDateProperty( "start-time", BundleDateFilterFunctions.START_TIME )
          .withStringProperty( "state", BundleFilterFunctions.STATE )
          .withDateProperty( "update-time", BundleDateFilterFunctions.UPDATE_TIME )
          .withPersistenceFilter( "error-code", "runtimeState.bundleTask.errorCode", Collections.<String>emptySet() )
          .withPersistenceFilter( "error-message", "runtimeState.bundleTask.errorMessage", Collections.<String>emptySet() )
          .withPersistenceFilter( "instance-id", "displayName" )
          .withPersistenceFilter( "progress", "runtimeState.bundleTask.progress", Collections.<String>emptySet(), ProgressToInteger.INSTANCE )
          .withPersistenceFilter( "s3-bucket", "runtimeState.bundleTask.bucket", Collections.<String>emptySet() )
          .withPersistenceFilter( "s3-prefix", "runtimeState.bundleTask.prefix", Collections.<String>emptySet() )
          .withPersistenceFilter( "start-time", "runtimeState.bundleTask.startTime", Collections.<String>emptySet(), PersistenceFilter.Type.Date )
          .withPersistenceFilter( "state", "runtimeState.bundleTask.state", Collections.<String>emptySet(), FUtils.valueOfFunction( VmBundleTask.BundleState.class ) )
          .withPersistenceFilter( "update-time", "runtimeState.bundleTask.updateTime", Collections.<String>emptySet(), PersistenceFilter.Type.Date )
      );
    }
  }

  private enum BundleDateFilterFunctions implements Function<VmBundleTask,Date> {
    START_TIME {
      @Override
      public Date apply( final VmBundleTask bundleTask ) {
        return bundleTask.getStartTime();
      }
    },
    UPDATE_TIME {
      @Override
      public Date apply( final VmBundleTask bundleTask ) {
        return bundleTask.getUpdateTime();
      }
    },
  }

  private enum BundleFilterFunctions implements Function<VmBundleTask,String> {
    BUNDLE_ID {
      @Override
      public String apply( final VmBundleTask bundleTask ) {
        return bundleTask.getBundleId();
      }
    },
    ERROR_CODE {
      @Override
      public String apply( final VmBundleTask bundleTask ) {
        return bundleTask.getErrorCode();
      }
    },
    ERROR_MESSAGE {
      @Override
      public String apply( final VmBundleTask bundleTask ) {
        return bundleTask.getErrorMessage();
      }
    },
    INSTANCE_ID {
      @Override
      public String apply( final VmBundleTask bundleTask ) {
        return bundleTask.getInstanceId();
      }
    },
    PROGRESS {
      @Override
      public String apply( final VmBundleTask bundleTask ) {
        return bundleTask.getProgress() + "%";
      }
    },
    S3_BUCKET {
      @Override
      public String apply( final VmBundleTask bundleTask ) {
        return bundleTask.getBucket();
      }
    },
    S3_PREFIX {
      @Override
      public String apply( final VmBundleTask bundleTask ) {
        return bundleTask.getPrefix();
      }
    },
    STATE {
      @Override
      public String apply( final VmBundleTask bundleTask ) {
        return bundleTask.getState().name();
      }
    },
  }

  private enum VmVolumeAttachmentBooleanFilterFunctions implements Function<VmVolumeAttachment,Boolean> {
    DELETE_ON_TERMINATE {
      @Override
      public Boolean apply( final VmVolumeAttachment volumeAttachment ) {
        return volumeAttachment.getDeleteOnTerminate();
      }
    },
  }

  private enum VmVolumeAttachmentDateFilterFunctions implements Function<VmVolumeAttachment,Date> {
    ATTACH_TIME {
      @Override
      public Date apply( final VmVolumeAttachment volumeAttachment ) {
        return volumeAttachment.getAttachTime();
      }
    },
  }

  private enum VmVolumeAttachmentFilterFunctions implements Function<VmVolumeAttachment,String> {
    DEVICE_NAME {
      @Override
      public String apply( final VmVolumeAttachment volumeAttachment ) {
        return volumeAttachment.getDevice();
      }
    },
    STATUS {
      @Override
      public String apply( final VmVolumeAttachment volumeAttachment ) {
        return volumeAttachment.getStatus();
      }
    },
    VOLUME_ID {
      @Override
      public String apply( final VmVolumeAttachment volumeAttachment ) {
        return volumeAttachment.getVolumeId();
      }
    },
  }

  private enum VmInstanceStringSetFilterFunctions implements Function<VmInstance,Set<String>> {
    ASSOCIATION_PUBLIC_IP {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.ASSOCIATION_PUBLIC_IP );
      }
    },
    ASSOCIATION_IP_OWNER_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.ASSOCIATION_IP_OWNER_ID );
      }
    },
    ASSOCIATION_ALLOCATION_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.ASSOCIATION_ALLOCATION_ID );
      }
    },
    ASSOCIATION_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.ASSOCIATION_ID );
      }
    },
    BLOCK_DEVICE_MAPPING_DEVICE_NAME {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return blockDeviceSet( instance, VmVolumeAttachmentFilterFunctions.DEVICE_NAME );
      }
    },
    BLOCK_DEVICE_MAPPING_STATUS {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return blockDeviceSet( instance, VmVolumeAttachmentFilterFunctions.STATUS );
      }
    },
    BLOCK_DEVICE_MAPPING_VOLUME_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return blockDeviceSet( instance, VmVolumeAttachmentFilterFunctions.VOLUME_ID );
      }
    },
    EVENT_CODE {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        final Optional<InstanceStatusEventType> eventInfo = VmInstance.StatusTransform.getEventInfo( instance );
        return eventInfo.isPresent( ) ?
            Collections.singleton( eventInfo.get( ).getCode( ) ) :
            Collections.<String>emptySet( );
      }
    },
    EVENT_DESCRIPTION {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        final Optional<InstanceStatusEventType> eventInfo = VmInstance.StatusTransform.getEventInfo( instance );
        return eventInfo.isPresent( ) ?
            Collections.singleton( eventInfo.get( ).getDescription( ) ) :
            Collections.<String>emptySet( );
      }
    },
    GROUP_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkGroupSet( instance, NetworkGroup.groupId() );
      }
    },
    GROUP_NAME {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkGroupSet( instance, CloudMetadatas.toDisplayName() );
      }
    },
    NETWORK_INTERFACE_DESCRIPTION {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.DESCRIPTION );
      }
    },
    NETWORK_INTERFACE_SUBNET_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.SUBNET_ID );
      }
    },
    NETWORK_INTERFACE_VPC_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.VPC_ID );
      }
    },
    NETWORK_INTERFACE_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, CloudMetadatas.toDisplayName() );
      }
    },
    NETWORK_INTERFACE_OWNER_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.OWNER_ID );
      }
    },
    NETWORK_INTERFACE_AVAILABILITY_ZONE {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.AVAILABILITY_ZONE );
      }
    },
    NETWORK_INTERFACE_STATUS {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.STATE );
      }
    },
    NETWORK_INTERFACE_MAC_ADDRESS {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.MAC_ADDRESS );
      }
    },
    NETWORK_INTERFACE_PRIVATE_IP {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.PRIVATE_IP );
      }
    },
    NETWORK_INTERFACE_PRIVATE_DNS_NAME {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.PRIVATE_DNS_NAME );
      }
    },
    NETWORK_INTERFACE_GROUP_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSetSet( instance, NetworkInterfaces.FilterStringSetFunctions.GROUP_ID );
      }
    },
    NETWORK_INTERFACE_GROUP_NAME {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSetSet( instance, NetworkInterfaces.FilterStringSetFunctions.GROUP_NAME );
      }
    },
    NETWORK_INTERFACE_ATTACHMENT_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.ATTACHMENT_ATTACHMENT_ID );
      }
    },
    NETWORK_INTERFACE_ATTACHMENT_INSTANCE_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.ATTACHMENT_INSTANCE_ID );
      }
    },
    NETWORK_INTERFACE_ATTACHMENT_INSTANCE_OWNER_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.ATTACHMENT_INSTANCE_OWNER_ID );
      }
    },
    NETWORK_INTERFACE_ATTACHMENT_STATUS {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.ATTACHMENT_STATUS );
      }
    },
    NETWORK_INTERFACE_ASSOCIATION_PUBLIC_IP {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.ASSOCIATION_PUBLIC_IP );
      }
    },
    NETWORK_INTERFACE_ASSOCIATION_IP_OWNER_ID {
      @Override
      public Set<String> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterStringFunctions.ASSOCIATION_IP_OWNER_ID );
      }
    },
  }

  private enum VmInstanceBooleanSetFilterFunctions implements Function<VmInstance,Set<Boolean>> {
    BLOCK_DEVICE_MAPPING_DELETE_ON_TERMINATE {
      @Override
      public Set<Boolean> apply( final VmInstance instance ) {
        return blockDeviceSet( instance, VmVolumeAttachmentBooleanFilterFunctions.DELETE_ON_TERMINATE );
      }
    },
    NETWORK_INTERFACE_ADDRESSES_PRIMARY {
      @Override
      public Set<Boolean> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, Functions.forSupplier( Suppliers.ofInstance( true ) ) );
      }
    },
    NETWORK_INTERFACE_ATTACHMENT_DELETE_ON_TERMINATION {
      @Override
      public Set<Boolean> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterBooleanFunctions.ATTACHMENT_DELETE_ON_TERMINATION );
      }
    },
    NETWORK_INTERFACE_SOURCE_DEST_CHECK {
      @Override
      public Set<Boolean> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterBooleanFunctions.SOURCE_DEST_CHECK );
      }
    },
  }

  private enum VmInstanceDateFilterFunctions implements Function<VmInstance,Date> {
    LAUNCH_TIME {
      @Override
      public Date apply( final VmInstance instance ) {
        return instance.getLaunchRecord().getLaunchTime();
      }
    },
  }

  private enum VmInstanceDateSetFilterFunctions implements Function<VmInstance,Set<Date>> {
    BLOCK_DEVICE_MAPPING_ATTACH_TIME {
      @Override
      public Set<Date> apply( final VmInstance instance ) {
        return blockDeviceSet( instance, VmVolumeAttachmentDateFilterFunctions.ATTACH_TIME );
      }
    },
    EVENT_NOT_AFTER {
      @Override
      public Set<Date> apply( final VmInstance instance ) {
        final Optional<InstanceStatusEventType> eventInfo = VmInstance.StatusTransform.getEventInfo( instance );
        return eventInfo.isPresent( ) ?
            Collections.singleton( eventInfo.get( ).getNotAfter( ) ) :
            Collections.<Date>emptySet( );
      }
    },
    EVENT_NOT_BEFORE {
      @Override
      public Set<Date> apply( final VmInstance instance ) {
        final Optional<InstanceStatusEventType> eventInfo = VmInstance.StatusTransform.getEventInfo( instance );
        return eventInfo.isPresent( ) ?
            Collections.singleton( eventInfo.get( ).getNotBefore( ) ) :
            Collections.<Date>emptySet( );
      }
    },
    NETWORK_INTERFACE_ATTACHMENT_ATTACH_TIME {
      @Override
      public Set<Date> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterDateFunctions.ATTACHMENT_ATTACH_TIME );
      }
    },
  }

  private enum VmInstanceIntegerFilterFunctions implements Function<VmInstance,Integer> {
    INSTANCE_STATE_CODE {
      @Override
      public Integer apply( final VmInstance instance ) {
        return instance.getDisplayState().getCode();
      }
    },
  }

  private enum VmInstanceIntegerSetFilterFunctions implements Function<VmInstance,Set<Integer>> {
    NETWORK_INTERFACE_ATTACHMENT_DEVICE_INDEX {
      @Override
      public Set<Integer> apply( final VmInstance instance ) {
        return networkInterfaceSet( instance, NetworkInterfaces.FilterIntegerFunctions.ATTACHMENT_DEVICE_INDEX );
      }
    },
  }

  private enum VmInstanceFilterFunctions implements Function<VmInstance,String> {
    ARCHITECTURE {
      @Override
      public String apply( final VmInstance instance ) {
        final BootableImageInfo imageInfo = instance.getBootRecord().getMachine();
        return imageInfo instanceof ImageInfo ?
            Strings.toString( ( (ImageInfo) imageInfo ).getArchitecture() ):
            null;
      }
    },
    AVAILABILITY_ZONE {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getPartition();
      }
    },
    DNS_NAME {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getDisplayPublicDnsName();
      }
    },
    IMAGE_ID {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getImageId();
      }
    },
    INSTANCE_PROFILE_ARN {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getIamInstanceProfileArn();
      }
    },
    INSTANCE_STATE_NAME {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getDisplayState( ) == null ? 
            null : 
            instance.getDisplayState( ).getName( );
      }
    },
    INSTANCE_STATUS {
      @Override
      public String apply( final VmInstance instance ) {
        return Objects.firstNonNull(
            instance.getRuntimeState( ).getInstanceStatus( ),
            VmRuntimeState.InstanceStatus.Ok ).toString( );
      }
    },
    INSTANCE_REACHABILITY_STATUS {
      @Override
      public String apply( final VmInstance instance ) {
        return Objects.firstNonNull(
            instance.getRuntimeState( ).getReachabilityStatus( ),
            VmRuntimeState.ReachabilityStatus.Passed ).toString( );
      }
    },
    INSTANCE_TYPE {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getVmType() == null ?
            null :
            instance.getVmType().getName( );
      }
    },
    IP_ADDRESS {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getDisplayPublicAddress();
      }
    },
    KERNEL_ID {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getKernelId();
      }
    },
    KEY_NAME {
      @Override
      public String apply( final VmInstance instance ) {
        return CloudMetadatas.toDisplayName().apply( instance.getKeyPair() );
      }
    },
    LAUNCH_INDEX {
      @Override
      public String apply( final VmInstance instance ) {
        return Strings.toString( instance.getLaunchRecord().getLaunchIndex() );
      }
    },
    MONITORING_STATE {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getMonitoring() ? "enabled" : "disabled";
      }
    },
    NODE_HOST { // Eucalyptus specific, not for filtering
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getServiceTag( )==null ? null : URI.create( instance.getServiceTag( ) ).getHost( );
      }
    },
    OWNER_ID {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getOwnerAccountNumber();
      }
    },
    PLATFORM {
      @Override
      public String apply( final VmInstance instance ) {
        return "windows".equals( instance.getPlatform() ) ? "windows" : "";
      }
    },
    PRIVATE_DNS_NAME {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getDisplayPrivateDnsName();
      }
    },
    PRIVATE_IP_ADDRESS {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getDisplayPrivateAddress();
      }
    },
    RAMDISK_ID {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getRamdiskId();
      }
    },
    REASON {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getRuntimeState() == null ?
            null :
            instance.getRuntimeState().getDisplayReason( );
      }
    },
    RESERVATION_ID {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getReservationId();
      }
    },
    ROOT_DEVICE_NAME {
      @Override
      public String apply( final VmInstance instance ) {
        final BootableImageInfo imageInfo = instance.getBootRecord().getMachine();
        return imageInfo == null ? null : imageInfo.getRootDeviceName();
      }
    },
    ROOT_DEVICE_TYPE {
      @Override
      public String apply( final VmInstance instance ) {
        final BootableImageInfo imageInfo = instance.getBootRecord().getMachine();
        return imageInfo == null ? null : imageInfo.getRootDeviceType();
      }
    },
    SERVICE_TAG { // Eucalyptus specific, not for filtering
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getServiceTag( );
      }
    },
    SUBNET_ID {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getSubnetId( );
      }
    },
    VIRTUALIZATION_TYPE {
      @Override
      public String apply( final VmInstance vmInstance ) {
        return vmInstance.getVirtualizationType();
      }
    },
    VPC_ID {
      @Override
      public String apply( final VmInstance instance ) {
        return instance.getVpcId();
      }
    },
  }

  private enum VmInstanceToVmBundleTask implements Function<VmInstance,VmBundleTask> {
    INSTANCE {
      @Override
      public VmBundleTask apply( final VmInstance vmInstance ) {
        return vmInstance.getRuntimeState() == null ? null : vmInstance.getRuntimeState().getBundleTask();
      }
    }
  }
}
