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

package com.eucalyptus.vmtypes;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_3_0;

import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.common.vm.VmTypesSupplier;
import com.eucalyptus.compute.common.CloudMetadata.VmTypeMetadata;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.compute.common.internal.util.InvalidMetadataException;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.compute.common.VmTypeDetails;
import com.eucalyptus.compute.common.internal.vmtypes.EphemeralDisk;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.compute.common.internal.images.BootableImageInfo;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.images.ImageManager;
import com.eucalyptus.compute.common.internal.images.MachineImageInfo;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.RestrictedTypes.Resolver;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ForwardingConcurrentMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

import com.eucalyptus.cluster.common.msgs.VmTypeInfo;

@ConfigurableClass( root = "cloud.vmtypes",
                    description = "Parameters controlling the definition of virtual machine types." )
public class VmTypes {
  private static Logger LOG = Logger.getLogger( VmTypes.class );

  static {
    VmTypesSupplier.init( VmTypes::listEnabled );
  }

  @ConfigurableField( description = "Default type used when no instance type is specified for run instances.",
                      initial = "t2.micro" )
  public static String         DEFAULT_TYPE_NAME        = "t2.micro";

  @ConfigurableField( description = "Format first ephemeral disk by default with ext3", initial = "false",
      changeListener = PropertyChangeListeners.IsBoolean.class)
  public static Boolean        FORMAT_EPHEMERAL_STORAGE = false;

  @ConfigurableField( description = "Merge non-root ephemeral disks", initial = "false",
      changeListener = PropertyChangeListeners.IsBoolean.class)
  public static Boolean        MERGE_EPHEMERAL_STORAGE = false;

  @ConfigurableField( description = "Format swap disk by default. The property will be deprecated in next major release.",
      initial = "false", changeListener = PropertyChangeListeners.IsBoolean.class)
  public static Boolean        FORMAT_SWAP = false;

  private static final Long    SWAP_SIZE_BYTES          = 512 * 1024L * 1024L; // swap is hardcoded at 512MB for now
  private static final long    MIN_EPHEMERAL_SIZE_BYTES = 61440;               // the smallest ext{2|3|4} partition possible
  private static final Function<PredefinedTypes,Boolean> DEFAULT_ENABLE =
      pt -> pt.getName( ).startsWith( "t2." ) || pt.getName( ).startsWith( "m5." );

  public enum SizeUnit {
    B(1024L * 1024L * 1024L),
    MiB(1024L),
    GiB(1L),
    ;

    private final long multiplier;

    SizeUnit(final long multiplier) {
      this.multiplier = multiplier;
    }

    public long convert( final long sizeInGiB ) {
      return multiplier * sizeInGiB;
    }
  }

  public static final class VmTypeEphemeralStorageLayout {
    private final int rootSize;
    private final int otherDiskCount;
    private final int otherDiskSize;
    private final String firstOtherDiskFormat;

    public VmTypeEphemeralStorageLayout(
        final int rootSize,
        final int otherDiskCount,
        final int otherDiskSize,
        final String firstOtherDiskFormat
    ) {
      this.rootSize = rootSize;
      this.otherDiskCount = otherDiskCount;
      this.otherDiskSize = otherDiskSize;
      this.firstOtherDiskFormat = firstOtherDiskFormat;
    }

    public long getRootDiskSize( final SizeUnit units ) {
      return units.convert(rootSize);
    }

    public int getOtherDiskCount() {
      return otherDiskCount;
    }

    public long getOtherDiskSize( final int index, final SizeUnit units ) {
      if ( index >= 0 && index < otherDiskCount ) {
        return units.convert(otherDiskSize);
      }
      return -1;
    }

    @Nonnull
    public String getOtherDiskFormat( final int index ) {
      if ( index == 0 ) {
        return firstOtherDiskFormat;
      }
      return EphemeralDisk.Format.none.toString();
    }
  }

  public static VmTypeEphemeralStorageLayout getEphemeralLayout( final VmType vmType, final BootableImageInfo img ) {
    final int vmTypeDisk = vmType.getDisk();
    final boolean windows = ImageMetadata.Platform.windows.equals( img.getPlatform( ) );
    final boolean ebs = !"instance-store".equals( img.getRootDeviceType( ) );

    int diskCount = Math.min( 24, Math.max( 0, MoreObjects.firstNonNull( vmType.getDiskCount( ), 0 ) ) );
    int diskSize = diskCount <= 1 ? vmType.getDisk( ) : vmTypeDisk / diskCount;
    int otherDiskCount = ebs ? diskCount : Math.max( 0, diskCount - 1 );
    int otherDiskSize = diskSize;

    if ( MERGE_EPHEMERAL_STORAGE && otherDiskCount > 1 ) {
      otherDiskSize = otherDiskSize * otherDiskCount;
      otherDiskCount = 1;
    }

    final int rootSize = ebs ? -1 : diskSize;
    final String firstOtherDiskFormat =
        FORMAT_EPHEMERAL_STORAGE && !windows ? EphemeralDisk.Format.ext3.toString() : EphemeralDisk.Format.none.toString();

    return new VmTypeEphemeralStorageLayout( rootSize, otherDiskCount, otherDiskSize, firstOtherDiskFormat );
  }

  private enum ClusterAvailability implements Predicate<ServiceConfiguration> {
    INSTANCE;

    @Override
    public boolean apply( ServiceConfiguration input ) {
      try {
        final Cluster cluster = Clusters.lookupAny( input );
        try ( final LockResource lock =
                  LockResource.tryLock( cluster.getGateLock( ).readLock( ), 20, TimeUnit.SECONDS ) ) {
          if ( lock.isLocked( ) ) {
            cluster.refreshResources( );
          }
        }
      } catch ( Exception ex ) {
        LOG.error( "Failed to reset availability for cluster: " + input + " because of " + ex.getMessage( ) );
        LOG.debug( "Failed to reset availability for cluster: " + input + " because of " + ex.getMessage( ), ex );
      }
      return true;
    }

    public static void reset( ) {
      Iterables.all( Topology.enabledServices( ClusterController.class ), ClusterAvailability.INSTANCE );
    }
    
  }

  public static boolean isUnorderedType( VmType vmType ) {
    return Iterables.any( VmTypes.listEnabled( ), vmType.orderedPredicate( ) );
  }
  
  public static VmType update( VmType newVmType ) throws NoSuchMetadataException {
    VmType vmType = VmTypes.lookup( newVmType.getName( ) );
    VmType resultType;
    if ( vmType != null ) {
      Registry.INSTANCE.replace( newVmType );
      //return canonical map reference of vm type
      resultType = Registry.get( newVmType.getDisplayName( ) );
    } else {
      Registry.INSTANCE.putIfAbsent( newVmType );
      resultType = Registry.get( newVmType.getDisplayName( ) );
    }
    ClusterAvailability.reset( );
    return resultType;
  }

  public static synchronized VmType lookup( String name ) throws NoSuchMetadataException {
    return Registry.get( name );
  }
  
  public static synchronized NavigableSet<VmType> list( ) {
    return Registry.list( );
  }

  public static synchronized NavigableSet<VmType> listEnabled( ) {
    return Registry.listEnabled( );
  }

  public static String defaultTypeName( ) {
    return DEFAULT_TYPE_NAME;
  }
  
  @Resolver( VmTypeMetadata.class )
  private enum VmTypeResolver implements Function<String, VmType> {
    INSTANCE;

    @Override
    public VmType apply( @Nullable String input ) {
      Entities.registerClose( VmType.class );
      try {
        VmType vmType = Entities.uniqueResult( VmType.named( input ) );
        Iterators.size( vmType.getEphemeralDisks().iterator() ); // Ensure materialized
        return vmType;
      } catch ( Exception ex ) {
        if ( !(ex instanceof NoSuchElementException) ) {
          LOG.error( ex );
          LOG.debug( ex, ex );
        } else {
          LOG.debug( "Instance type not found for " + input );
        }
        PredefinedTypes t = PredefinedTypes.valueOf( input.toUpperCase( ).replace( ".", "" ) );
        VmType vmType = VmType.create( input, t.getCpu( ), t.getDisk( ), t.getDiskCount(),
                                       t.getMemory( ), t.getEthernetInterfaceLimit( ),
                                       DEFAULT_ENABLE.apply( t ) );
        vmType = Entities.persist( vmType );
        Iterators.size( vmType.getEphemeralDisks().iterator() ); // Ensure materialized
        return vmType;
      }
    }
  }
  
  private static class PersistentMap<K, V> extends ForwardingConcurrentMap<K, V> {
    private static class Persister<V> implements Function<V, V> {
      @Override
      public V apply( @Nullable V input ) {
        try {
          return Entities.mergeDirect( input );
        } catch ( Exception ex ) {
          return null;
        }
      }
    }

    private static class Deleter<V> implements Predicate<V> {
      @Override
      public boolean apply( @Nullable V input ) {
        try {
          Entities.delete( input );
          return true;
        } catch ( Exception ex ) {
          return false;
        }
      }
    }
    
    private final ConcurrentNavigableMap<K, V> backingMap = new ConcurrentSkipListMap<K, V>( );
    private final Function<K, V>               getFunction;
    private final Function<V, V>               putFunction;
    private final Predicate<V>                 removeFunction;
    
    private PersistentMap( Function<K, V> getFunction ) {
      super( );
      Class valueType = Classes.genericsToClasses( getFunction ).get( 1 );
      this.getFunction = Entities.asTransaction( getFunction );
      this.putFunction = Entities.asTransaction( valueType, new Persister<V>( ) );
      this.removeFunction = Entities.asTransaction( valueType, new Deleter<V>( ) );
    }

    public static <K, V> ConcurrentMap<K, V> create( Function<K, V> getFunction ) {
      return new PersistentMap<K, V>( getFunction );
    }

    @Override
    protected ConcurrentMap<K, V> delegate( ) {
      return this.backingMap;
    }
    
    @Override
    public V remove( Object object ) {
      V ret;
      if ( ( ret = this.delegate( ).remove( object ) ) != null ) {
        this.removeFunction.apply( ( V ) object );
      }
      return ret;
    }

    @Override
    public boolean remove( Object key, Object value ) {
      if ( this.delegate( ).containsKey( key ) && this.delegate( ).get( key ).equals( value ) && this.removeFunction.apply( ( V ) value ) ) {
        return this.delegate( ).remove( key, value );
      } else {
        return false;
      }
    }
    
    @Override
    public V get( Object key ) {
      if ( !this.delegate( ).containsKey( key ) ) {
        V value = this.getFunction.apply( ( K ) key );
        this.delegate( ).put( ( K ) key, value );
      }
      return this.delegate( ).get( key );
    }

    @Override
    public V put( K key, V value ) {
      value = this.putFunction.apply( value );
      V oldValue = this.delegate( ).put( key, value );
      return oldValue;
    }
    
    @Override
    public V putIfAbsent( K key, V value ) {
      if ( !this.delegate( ).containsKey( key ) ) {
        return this.put( key, value );
      } else {
        return this.get( key );
      }
    }
    
    @Override
    public V replace( K key, V value ) {
      if ( this.delegate( ).containsKey( key ) ) {
        return this.put( key, value );
      } else return null;
    }
    
    @Override
    public boolean replace( K key, V oldValue, V newValue ) {
      if ( this.containsKey( key ) && this.get( key ).equals( oldValue ) ) {
        this.put( key, newValue );
        return true;
      } else return false;
    }
    
  }
  
  private enum Registry {
    INSTANCE;
    private final ConcurrentMap<String, VmType>                          vmTypeMap = PersistentMap.create( VmTypeResolver.INSTANCE );
    private final AtomicMarkableReference<ConcurrentMap<String, VmType>> ref       = new AtomicMarkableReference<ConcurrentMap<String, VmType>>( null, false );
    
    private void initialize( ) {
      if ( this.ref.compareAndSet( null, vmTypeMap, false, true ) || vmTypeMap.size( ) != PredefinedTypes.values( ).length ) {
        for ( PredefinedTypes preDefVmType : PredefinedTypes.values( ) ) {
          VmType vmType = this.vmTypeMap.get( preDefVmType.getName( ) );
        }
        this.ref.set( vmTypeMap, true );
      } else if ( this.ref.compareAndSet( vmTypeMap, vmTypeMap, true, true ) ) {
        if ( this.vmTypeMap.size( ) != PredefinedTypes.values( ).length ) {
          for ( PredefinedTypes preDefVmType : PredefinedTypes.values( ) ) {
            if ( !this.vmTypeMap.containsKey( preDefVmType.getName() ) ) {
              this.vmTypeMap.putIfAbsent(
                preDefVmType.getName( ),
                VmType.create(
                    preDefVmType.getName( ),
                    preDefVmType.getCpu( ),
                    preDefVmType.getDisk( ),
                    preDefVmType.getDiskCount( ),
                    preDefVmType.getMemory( ),
                    preDefVmType.getEthernetInterfaceLimit( ),
                    DEFAULT_ENABLE.apply( preDefVmType )
                ) );
            }
          }
        }
      }
    }
    
    public VmType putIfAbsent( VmType vmType ) {
      INSTANCE.initialize( );
      return INSTANCE.vmTypeMap.putIfAbsent( vmType.getDisplayName( ), vmType );
    }

    public void replace( VmType newVmType ) {
      INSTANCE.initialize( );
      INSTANCE.vmTypeMap.replace( newVmType.getDisplayName( ), newVmType );
    }

    static VmType get( String name ) throws NoSuchMetadataException {
      INSTANCE.initialize( );
      name = ( name == null ? VmTypes.DEFAULT_TYPE_NAME : name );
      VmType ret = null;
      if ( !INSTANCE.vmTypeMap.containsKey( name ) ) {
        throw new NoSuchMetadataException( "Instance type does not exist: " + name );
      } else {
        return INSTANCE.vmTypeMap.get( name );
      }
    }
    
    public static NavigableSet<VmType> list( ) {
      INSTANCE.initialize( );
      return Sets.newTreeSet( INSTANCE.vmTypeMap.values( ) );
    }

    public static NavigableSet<VmType> listEnabled( ) {
      INSTANCE.initialize( );
      return Sets.newTreeSet( Iterables.filter( INSTANCE.vmTypeMap.values( ), VmType::getEnabled ) );
    }
  }
  
  /**
   * <table>
   * <tr>
   * <th>Name</th>
   * <th>Virtual Cores</th>
   * <th>Instance Store Disk (GiB)</th>
   * <th>Memory (MiB)</th>
   * <th>Network Interface Count</th>
   * </tr>
   * </table>
   */
  protected enum PredefinedTypes {
    C1MEDIUM("c1.medium", 2, 350, 1, 1741, 2),
    C1XLARGE("c1.xlarge", 8, 1680, 4, 7168, 4),
    C32XLARGE("c3.2xlarge", 8, 160, 2, 15360, 4),
    C34XLARGE("c3.4xlarge", 16, 320, 2, 30720, 8),
    C38XLARGE("c3.8xlarge", 32, 640, 2, 61440, 8),
    C3LARGE("c3.large", 2, 32, 2, 3840, 3),
    C3XLARGE("c3.xlarge", 4, 80, 2, 7680, 4),
    C42XLARGE("c4.2xlarge", 8, 20, 0, 15360, 4),
    C44XLARGE("c4.4xlarge", 16, 20, 0, 30720, 8),
    C48XLARGE("c4.8xlarge", 36, 40, 0, 61440, 8),
    C4LARGE("c4.large", 2, 10, 0, 3840, 3),
    C4XLARGE("c4.xlarge", 4, 15, 0, 7680, 4),
    C518XLARGE("c5.18xlarge", 72, 80, 0, 147456, 15),
    C52XLARGE("c5.2xlarge", 8, 20, 0, 16384, 4),
    C54XLARGE("c5.4xlarge", 16, 20, 0, 32768, 8),
    C59XLARGE("c5.9xlarge", 36, 40, 0, 73728, 8),
    C5D18XLARGE("c5d.18xlarge", 72, 1800, 2, 147456, 15),
    C5D2XLARGE("c5d.2xlarge", 8, 200, 1, 16384, 4),
    C5D4XLARGE("c5d.4xlarge", 16, 400, 1, 32768, 8),
    C5D9XLARGE("c5d.9xlarge", 36, 900, 1, 73728, 8),
    C5DLARGE("c5d.large", 2, 50, 1, 4096, 3),
    C5DXLARGE("c5d.xlarge", 4, 100, 1, 8192, 4),
    C5LARGE("c5.large", 2, 10, 0, 4096, 3),
    C5XLARGE("c5.xlarge", 4, 15, 0, 8192, 4),
    CC28XLARGE("cc2.8xlarge", 32, 3360, 4, 61952, 8),
    CG14XLARGE("cg1.4xlarge", 16, 200, 1, 12288, 8),
    CR18XLARGE("cr1.8xlarge", 32, 240, 2, 249856, 8),
    D22XLARGE("d2.2xlarge", 8, 12000, 6, 62464, 4),
    D24XLARGE("d2.4xlarge", 16, 24000, 12, 124928, 8),
    D28XLARGE("d2.8xlarge", 36, 48000, 24, 249856, 8),
    D2XLARGE("d2.xlarge", 4, 6000, 3, 31232, 4),
    F116XLARGE("f1.16xlarge", 64, 3760, 4, 999424, 8),
    F12XLARGE("f1.2xlarge", 8, 470, 1, 124928, 4),
    F14XLARGE("f1.4xlarge", 16, 940, 1, 249856, 4),
    G22XLARGE("g2.2xlarge", 8, 60, 1, 15360, 4),
    G28XLARGE("g2.8xlarge", 32, 240, 2, 61440, 8),
    G316XLARGE("g3.16xlarge", 64, 160, 0, 499712, 15),
    G34XLARGE("g3.4xlarge", 16, 40, 0, 124928, 8),
    G38XLARGE("g3.8xlarge", 32, 80, 0, 249856, 8),
    H116XLARGE("h1.16xlarge", 64, 16000, 8, 262144, 15),
    H12XLARGE("h1.2xlarge", 8, 2000, 1, 32768, 4),
    H14XLARGE("h1.4xlarge", 16, 4000, 2, 65536, 8),
    H18XLARGE("h1.8xlarge", 32, 8000, 4, 131072, 8),
    HI14XLARGE("hi1.4xlarge", 48, 2000, 2, 119808, 8),
    HS18XLARGE("hs1.8xlarge", 16, 48000, 24, 119808, 8),
    I22XLARGE("i2.2xlarge", 8, 1600, 2, 62464, 4),
    I24XLARGE("i2.4xlarge", 16, 3200, 4, 124928, 8),
    I28XLARGE("i2.8xlarge", 32, 6400, 8, 249856, 8),
    I2XLARGE("i2.xlarge", 4, 800, 1, 31232, 4),
    I316XLARGE("i3.16xlarge", 64, 15200, 8, 499712, 15),
    I32XLARGE("i3.2xlarge", 8, 1900, 1, 62464, 4),
    I34XLARGE("i3.4xlarge", 16, 3800, 2, 124928, 8),
    I38XLARGE("i3.8xlarge", 32, 7600, 4, 249856, 8),
    I3LARGE("i3.large", 2, 475, 1, 15616, 3),
    I3XLARGE("i3.xlarge", 4, 950, 1, 31232, 4),
    M1LARGE("m1.large", 2, 840, 2, 7680, 3),
    M1MEDIUM("m1.medium", 1, 410, 1, 3840, 2),
    M1SMALL("m1.small", 1, 160, 1, 1741, 2),
    M1XLARGE("m1.xlarge", 4, 1680, 4, 15360, 4),
    M22XLARGE("m2.2xlarge", 4, 850, 1, 35021, 4),
    M24XLARGE("m2.4xlarge", 8, 1680, 2, 70042, 8),
    M2XLARGE("m2.xlarge", 2, 420, 1, 17510, 4),
    M32XLARGE("m3.2xlarge", 8, 160, 2, 30720, 4),
    M3LARGE("m3.large", 2, 32, 1, 7680, 3),
    M3MEDIUM("m3.medium", 1, 4, 1, 3840, 2),
    M3XLARGE("m3.xlarge", 4, 80, 2, 15360, 4),
    M410XLARGE("m4.10xlarge", 40, 40, 0, 163840, 8),
    M416XLARGE("m4.16xlarge", 64, 60, 0, 262144, 8),
    M42XLARGE("m4.2xlarge", 8, 20, 0, 32768, 4),
    M44XLARGE("m4.4xlarge", 16, 20, 0, 65536, 8),
    M4LARGE("m4.large", 2, 10, 0, 8192, 2),
    M4XLARGE("m4.xlarge", 4, 15, 0, 16384, 4),
    M512XLARGE("m5.12xlarge", 48, 50, 0, 196608, 8),
    M524XLARGE("m5.24xlarge", 96, 100, 0, 393216, 15),
    M52XLARGE("m5.2xlarge", 8, 20, 0, 32768, 4),
    M54XLARGE("m5.4xlarge", 16, 20, 0, 65536, 8),
    M5D12XLARGE("m5d.12xlarge", 48, 1800, 2, 196608, 8),
    M5D24XLARGE("m5d.24xlarge", 96, 3600, 4, 393216, 15),
    M5D2XLARGE("m5d.2xlarge", 8, 300, 1, 32768, 4),
    M5D4XLARGE("m5d.4xlarge", 16, 600, 2, 65536, 8),
    M5DLARGE("m5d.large", 2, 75, 1, 8192, 3),
    M5DXLARGE("m5d.xlarge", 4, 150, 1, 16384, 4),
    M5LARGE("m5.large", 2, 10, 0, 8192, 3),
    M5XLARGE("m5.xlarge", 4, 15, 0, 16384, 4),
    P216XLARGE("p2.16xlarge", 64, 60, 0, 749568, 8),
    P28XLARGE("p2.8xlarge", 32, 40, 0, 499712, 8),
    P2XLARGE("p2.xlarge", 4, 20, 0, 62464, 4),
    P316XLARGE("p3.16xlarge", 64, 80, 0, 499712, 8),
    P32XLARGE("p3.2xlarge", 8, 20, 0, 62464, 4),
    P38XLARGE("p3.8xlarge", 32, 40, 0, 249856, 8),
    R32XLARGE("r3.2xlarge", 8, 160, 1, 62464, 4),
    R34XLARGE("r3.4xlarge", 16, 320, 1, 124928, 8),
    R38XLARGE("r3.8xlarge", 32, 640, 2, 249856, 8),
    R3LARGE("r3.large", 2, 32, 1, 15616, 3),
    R3XLARGE("r3.xlarge", 4, 80, 1, 31232, 4),
    R416XLARGE("r4.16xlarge", 64, 60, 0, 499712, 15),
    R42XLARGE("r4.2xlarge", 8, 20, 0, 62464, 4),
    R44XLARGE("r4.4xlarge", 16, 20, 0, 124928, 8),
    R48XLARGE("r4.8xlarge", 32, 40, 0, 249856, 8),
    R4LARGE("r4.large", 2, 10, 0, 15616, 3),
    R4XLARGE("r4.xlarge", 4, 15, 0, 31232, 4),
    R512XLARGE("r5.12xlarge", 48, 50, 0, 393216, 8),
    R524XLARGE("r5.24xlarge", 96, 100, 0, 786432, 15),
    R52XLARGE("r5.2xlarge", 8, 20, 0, 65536, 4),
    R54XLARGE("r5.4xlarge", 16, 20, 0, 131072, 8),
    R5D12XLARGE("r5d.12xlarge", 48, 1800, 2, 393216, 8),
    R5D24XLARGE("r5d.24xlarge", 96, 3600, 4, 786432, 15),
    R5D2XLARGE("r5d.2xlarge", 8, 300, 1, 65536, 4),
    R5D4XLARGE("r5d.4xlarge", 16, 600, 2, 131072, 8),
    R5DLARGE("r5d.large", 2, 75, 1, 16384, 3),
    R5DXLARGE("r5d.xlarge", 4, 150, 1, 32768, 4),
    R5LARGE("r5.large", 2, 10, 0, 16384, 3),
    R5XLARGE("r5.xlarge", 4, 15, 0,32768, 4),
    T1MICRO("t1.micro", 1, 5, 0, 628, 2),
    T22XLARGE("t2.2xlarge", 8, 20, 0, 32768, 3),
    T2LARGE("t2.large", 2, 15, 0, 8192, 3),
    T2MEDIUM("t2.medium", 2, 10, 0, 4096, 3),
    T2MICRO("t2.micro", 1, 10, 0, 1024, 2),
    T2NANO("t2.nano", 1, 5, 0, 512, 2),
    T2SMALL("t2.small", 1, 10, 0, 2048, 2),
    T2XLARGE("t2.xlarge", 4, 15, 0, 16384, 3),
    T32XLARGE("t3.2xlarge", 8, 20, 0, 32768, 4),
    T3LARGE("t3.large", 2, 15, 0, 8192, 3),
    T3MEDIUM("t3.medium", 2, 10, 0, 4096, 3),
    T3MICRO("t3.micro", 2, 10, 0, 1024, 2),
    T3NANO("t3.nano", 2, 5, 0, 512, 2),
    T3SMALL("t3.small", 2, 10, 0, 2048, 2),
    T3XLARGE("t3.xlarge", 4, 15, 0, 16384, 4),
    X116XLARGE("x1.16xlarge", 64, 1920, 1, 999424, 8),
    X132XLARGE("x1.32xlarge", 128, 3840, 2, 1998848, 8),
    X1E16XLARGE("x1e.16xlarge", 64, 1920, 1, 1998848, 8),
    X1E2XLARGE("x1e.2xlarge", 8, 240, 1, 249856, 4),
    X1E32XLARGE("x1e.32xlarge", 128, 3840, 2, 3997696, 8),
    X1E4XLARGE("x1e.4xlarge", 16, 480, 1, 499712, 4),
    X1E8XLARGE("x1e.8xlarge", 32, 960, 1, 999424, 4),
    X1EXLARGE("x1e.xlarge", 4, 120, 1, 124928, 3),
    Z1D12XLARGE("z1d.12xlarge", 48, 1800, 2, 393216, 15),
    Z1D2XLARGE("z1d.2xlarge", 8, 300, 1, 65536, 4),
    Z1D3XLARGE("z1d.3xlarge", 12, 450, 1, 98304, 8),
    Z1D6XLARGE("z1d.6xlarge", 24, 900, 1, 196608, 8),
    Z1DLARGE("z1d.large", 2, 75, 1, 16384, 3),
    Z1DXLARGE("z1d.xlarge", 4, 150, 1, 32768, 4),
    ;
    private final String             name;
    private final Integer            cpu;
    private final Integer            disk;
    private final Integer            diskCount;
    private final Integer            memory;
    private final Integer            ethernetInterfaceLimit;

    PredefinedTypes( String name, Integer cpu, Integer disk, Integer diskCount, Integer memory, Integer ethernetInterfaceLimit ) {
      this.name = name;
      this.cpu = cpu;
      this.disk = disk;
      this.diskCount = diskCount;
      this.memory = memory;
      this.ethernetInterfaceLimit = ethernetInterfaceLimit;
    }

    public String getName( ) {
      return this.name;
    }
    
    public Integer getCpu( ) {
      return this.cpu;
    }
    
    public Integer getDisk( ) {
      return this.disk;
    }

    /**
     * Number of disks for the instance type.
     *
     * This can be 0 for ebs only instance-types, in which case there will still be one disk of the
     * specified size when running as instance-store, but no ephemeral disks are available when running
     * an ebs instance.
     */
    public Integer getDiskCount( ) {
      return this.diskCount;
    }

    public Integer getMemory( ) {
      return this.memory;
    }
    
    public Integer getEthernetInterfaceLimit( ) {
      return this.ethernetInterfaceLimit;
    }

  }
  
  public static VmTypeInfo asVmTypeInfo(
      final VmType vmType,
      final BootableImageInfo img,
      final Supplier<Pair<String,String>> rootInfoSupplier
  ) throws MetadataException {
    final Long imgSize = img.getImageSizeBytes( );
    final VmTypeEphemeralStorageLayout ephemeralLayout = getEphemeralLayout( vmType, img );

    if ( !( img instanceof BlockStorageImageInfo ) && imgSize > ephemeralLayout.getRootDiskSize(SizeUnit.B) ) {
      throw new InvalidMetadataException( "image too large [size=" + imgSize / ( 1024L * 1024L ) + "MB] for instance type " + vmType.getName( ) + " [disk="
                                          + ephemeralLayout.getRootDiskSize(SizeUnit.MiB) + "MB]" );
    }
    VmTypeInfo vmTypeInfo;
    if ( img instanceof MachineImageInfo ) { // instance-store image
      final Pair<String,String> nameAndManifestLocation = rootInfoSupplier.get( );
      if ( ImageMetadata.Platform.windows.equals( img.getPlatform( ) ) ) {
        vmTypeInfo = VmTypes.InstanceStoreWindowsVmTypeInfoMapper.INSTANCE.apply( vmType );
      } else if( !ImageManager.isPathAPartition( img.getRootDeviceName() ) ){
        vmTypeInfo = VmTypes.InstanceStoreLinuxHvmVmTypeInfoMapper.INSTANCE.apply(vmType);
        vmTypeInfo.setRoot(
            nameAndManifestLocation.getLeft(),
            nameAndManifestLocation.getRight(),
            ephemeralLayout.getRootDiskSize(SizeUnit.B) );
      } else {
        vmTypeInfo = VmTypes.InstanceStoreVmTypeInfoMapper.INSTANCE.apply( vmType );
        vmTypeInfo.setRoot(
            nameAndManifestLocation.getLeft(),
            nameAndManifestLocation.getRight(),
            imgSize );
        long ephemeralSize = ephemeralLayout.getRootDiskSize(SizeUnit.B) - imgSize - SWAP_SIZE_BYTES;
        if ( ephemeralSize < MIN_EPHEMERAL_SIZE_BYTES ) {
          throw new InvalidMetadataException( "image too large to accommodate swap and ephemeral [size="
                                              + imgSize
                                              / ( 1024L * 1024L )
                                              + "MB] for instance type "
                                              + vmType.getName( )
                                              + " [disk="
                                              + ephemeralLayout.getRootDiskSize(SizeUnit.MiB)
                                              + "MB]" );
        }
        vmTypeInfo.addEphemeral( "sda2", ephemeralSize,
            FORMAT_EPHEMERAL_STORAGE ? EphemeralDisk.Format.ext3.toString() : EphemeralDisk.Format.none.toString());
      }

      for ( int i=0; i < ephemeralLayout.getOtherDiskCount( ); i++ ) {
        vmTypeInfo.addEphemeral("sd" + ((char)('b' + i)),
            ephemeralLayout.getOtherDiskSize( i, SizeUnit.B ),
            ephemeralLayout.getOtherDiskFormat( i ) );
      }
    } else if ( img instanceof BlockStorageImageInfo ) { // bfEBS
      vmTypeInfo = VmTypes.BlockStorageVmTypeInfoMapper.INSTANCE.apply( vmType );
      vmTypeInfo.setRootDeviceName(img.getRootDeviceName());
      vmTypeInfo.setEbsRoot( img.getDisplayName( ), null, imgSize );
    } else {
      throw new InvalidMetadataException( "Failed to identify the root machine image type: " + img );
    }
    return vmTypeInfo;
  }
  
  private enum InstanceStoreVmTypeInfoMapper implements Function<VmType, VmTypeInfo> {
    INSTANCE;
    
    @Override
    public VmTypeInfo apply( VmType arg0 ) {
      return new VmTypeInfo( arg0.getName( ), arg0.getMemory( ), arg0.getDisk( ), arg0.getCpu( ), "sda1" ) {
        {
          this.setSwap( "sda3", VmTypes.SWAP_SIZE_BYTES, VmTypes.FORMAT_SWAP ? "swap" : "none" );
        }
      };
    }
  };
  
  private enum InstanceStoreWindowsVmTypeInfoMapper implements Function<VmType, VmTypeInfo> {
    INSTANCE;
    
    @Override
    public VmTypeInfo apply( VmType arg0 ) {
      return new VmTypeInfo( arg0.getName( ), arg0.getMemory( ), arg0.getDisk( ), arg0.getCpu( ), "sda" );
    }
  };
  
  private enum InstanceStoreLinuxHvmVmTypeInfoMapper implements Function<VmType, VmTypeInfo> {
	    INSTANCE;
	    
	    @Override
	    public VmTypeInfo apply( VmType arg0 ) {
	      return new VmTypeInfo( arg0.getName( ), arg0.getMemory( ), arg0.getDisk( ), arg0.getCpu( ), "sda" );
	    }
	  };
  
  private enum BlockStorageVmTypeInfoMapper implements Function<VmType, VmTypeInfo> {
    INSTANCE;
    
    @Override
    public VmTypeInfo apply( VmType arg0 ) {
      return new VmTypeInfo( arg0.getName( ), arg0.getMemory( ), arg0.getDisk( ), arg0.getCpu( ), "sda" );
    }
  };

  @TypeMapper
  public enum VmTypeToVmTypeDetails implements Function<VmType, VmTypeDetails> {
    INSTANCE;

    @Override
    @Nonnull
    public VmTypeDetails apply( final VmType vmType ) {
      final VmTypeDetails vmTypeDetails = new VmTypeDetails();
      vmTypeDetails.setName( vmType.getName( ) );
      vmTypeDetails.setCpu( vmType.getCpu( ) );
      vmTypeDetails.setDisk( vmType.getDisk( ) );
      vmTypeDetails.setDiskCount( vmType.getDiskCount( ) );
      vmTypeDetails.setMemory( vmType.getMemory( ) );
      vmTypeDetails.setNetworkInterfaces( vmType.getNetworkInterfaces( ) );
      return vmTypeDetails;
    }
  }

  @EntityUpgrade( entities = VmType.class,  since = v4_3_0, value = Compute.class )
  public enum VmType430Upgrade implements Predicate<Class> {
    INSTANCE;
    private static Logger logger = Logger.getLogger( VmType430Upgrade.class );

    @Override
    public boolean apply( Class entityClass ) {
      try ( final TransactionResource tx = Entities.transactionFor( VmType.class ) ) {
        for ( final VmType type : Entities.criteriaQuery( VmType.class ).list( ) ) {
          final Optional<PredefinedTypes> predefinedType =
              Enums.getIfPresent( PredefinedTypes.class, type.getName( ).toUpperCase( ).replace( ".", "" ) );
          if ( predefinedType.isPresent( ) && type.getNetworkInterfaces( ) == null ) {
            final Integer networkInterfaces = predefinedType.get( ).getEthernetInterfaceLimit( );
            logger.info( "Updating instance type " + type.getName( ) + " with max enis " + networkInterfaces );
            type.setNetworkInterfaces( networkInterfaces );
          }
        }
        tx.commit( );
      }
      return true;
    }
  }
}
