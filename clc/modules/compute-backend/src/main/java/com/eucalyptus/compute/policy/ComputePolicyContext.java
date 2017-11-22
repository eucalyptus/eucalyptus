/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.compute.policy;

import java.util.Date;
import javax.annotation.Nullable;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshots;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import io.vavr.Tuple;
import io.vavr.Tuple3;

/**
 *
 */
public class ComputePolicyContext {
  private final static ThreadLocal<ComputePolicyContextResource> resourceLocal = new ThreadLocal<>();
  private final static ThreadLocal<Tuple3<String,Class<? extends CloudMetadata>,String>> resourceIdLocal =
      new ThreadLocal<Tuple3<String,Class<? extends CloudMetadata>,String>>( ){
        @Override
        protected Tuple3<String,Class<? extends CloudMetadata>,String> initialValue() {
          return Tuple.of( null, null, null );
        }
      };

  static void clearContext( ) {
    resourceIdLocal.set( Tuple.of( null, null, null ) );
    resourceLocal.set( null );
  }

  static void setComputePolicyContextResource(
      @Nullable final String resourceAccountNumber,
      @Nullable final Class<? extends CloudMetadata> resourceClass,
      @Nullable final String resourceId,
      @Nullable final ComputePolicyContextResource resource
  ) {
    resourceIdLocal.set( Tuple.of( resourceAccountNumber, resourceClass, resourceId ) );
    resourceLocal.set( resource );
  }

  @Nullable
  static String getResourceAccountNumber( ) {
    return resourceIdLocal.get( )._1;
  }

  @Nullable
  static Class<? extends CloudMetadata> getResourceType( ) {
    return resourceIdLocal.get( )._2;
  }

  @Nullable
  static String getPolicyResourceType( ) {
    String policyResourceType = null;
    final Class<? extends CloudMetadata> type = getResourceType( );
    if ( type != null ) {
      policyResourceType = Ats.inClassHierarchy( type ).getOption( PolicyResourceType.class )
          .map( PolicyResourceType::value )
          .getOrElse( (String)null );
    }
    return policyResourceType;
  }

  @Nullable
  static String getResourceId( ) {
    return resourceIdLocal.get( )._3;
  }

  @Nullable
  static String getAvailabilityZone( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getAvailabilityZone( );
  }

  @Nullable
  static Boolean isEbsOptimized( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.isEbsOptimized();
  }

  @Nullable
  static Boolean isPublic( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.isPublic();
  }

  @Nullable
  static String getInstanceProfileArn( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getInstanceProfileArn();
  }

  @Nullable
  static String getImageType( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getImageType();
  }

  @Nullable
  static String getInstanceType( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getInstanceType( );
  }

  @Nullable
  static String getOwner( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getOwner();
  }

  @Nullable
  static String getParentSnapshotArn( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getParentSnapshotArn( );
  }

  @Nullable
  static String getParentVolumeArn( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getParentVolumeArn();
  }

  @Nullable
  static String getPlacementGroupArn( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getPlacementGroupArn();
  }

  @Nullable
  static String getRegion( ) {
    return RegionConfigurations.getRegionName( ).or( "eucalyptus" );
  }

  @Nullable
  static String getRootDeviceType( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getRootDeviceType();
  }

  @Nullable
  static Date getSnapshotTime() {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getSnapshotTime();
  }

  @Nullable
  static String getSubnetArn() {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getSubnetArn();
  }

  @Nullable
  static String getTenancy( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getTenancy();
  }

  @Nullable
  static Boolean getVolumeEncrypted() {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getVolumeEncrypted();
  }

  @Nullable
  static Integer getVolumeIops() {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getVolumeIops();
  }

  @Nullable
  static Integer getVolumeSize() {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getVolumeSize();
  }

  @Nullable
  static String getVolumeType() {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getVolumeType();
  }

  @Nullable
  static String getVpcArn() {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getVpcArn();
  }

  public interface ComputePolicyContextResource {
    @Nullable
    String getAvailabilityZone( );

    @Nullable
    Boolean isEbsOptimized( );

    @Nullable
    Boolean isPublic( );

    @Nullable
    String getInstanceProfileArn( );

    @Nullable
    String getImageType( );

    @Nullable
    String getInstanceType( );

    @Nullable
    String getOwner( );

    @Nullable
    String getParentSnapshotArn( );

    @Nullable
    String getParentVolumeArn( );

    @Nullable
    String getPlacementGroupArn( );

    @Nullable
    String getRootDeviceType( );

    @Nullable
    Date getSnapshotTime( );

    @Nullable
    String getSubnetArn( );

    @Nullable
    String getTenancy( );

    @Nullable
    Boolean getVolumeEncrypted( );

    @Nullable
    Integer getVolumeIops( );

    @Nullable
    Integer getVolumeSize( );

    @Nullable
    String getVolumeType( );

    @Nullable
    String getVpcArn( );
  }

  public static class ComputePolicyContextResourceSupport implements ComputePolicyContextResource {
    @Override
    @Nullable
    public String getAvailabilityZone( ) {
      return null;
    }

    @Override
    @Nullable
    public Boolean isEbsOptimized( ) {
      return null;
    }

    @Override
    @Nullable
    public Boolean isPublic( ) {
      return null;
    }

    @Override
    @Nullable
    public String getInstanceProfileArn( ) {
      return null;
    }

    @Override
    @Nullable
    public String getImageType( ) {
      return null;
    }

    @Override
    @Nullable
    public String getInstanceType( ) {
      return null;
    }

    @Override
    @Nullable
    public String getOwner( ) {
      return null;
    }

    @Override
    @Nullable
    public String getParentSnapshotArn( ) {
      return null;
    }

    @Nullable
    @Override
    public String getParentVolumeArn( ) {
      return null;
    }

    @Override
    @Nullable
    public String getPlacementGroupArn( ) {
      return null;
    }

    @Override
    @Nullable
    public String getRootDeviceType( ) {
      return null;
    }

    @Nullable
    @Override
    public Date getSnapshotTime() {
      return null;
    }

    @Nullable
    @Override
    public String getSubnetArn() {
      return null;
    }

    @Override
    @Nullable
    public String getTenancy( ) {
      return null;
    }

    @Override
    @Nullable
    public Boolean getVolumeEncrypted( ) {
      return null;
    }

    @Override
    @Nullable
    public Integer getVolumeIops( ) {
      return null;
    }

    @Override
    @Nullable
    public Integer getVolumeSize( ) {
      return null;
    }

    @Override
    @Nullable
    public String getVolumeType( ) {
      return null;
    }

    @Override
    @Nullable
    public String getVpcArn( ) {
      return null;
    }

    @Nullable
    protected final String snapshotIdToArn( @Nullable final String snapshotId ) {
      String arn = null;

      if ( snapshotId != null ) {
        String accountNumber = "";
        try {
          accountNumber = Snapshots.lookup( null, snapshotId ).getOwnerAccountNumber( );
        } catch ( Exception e ) {
          Logs.exhaust().debug( "Snapshot not found for ARN: " + snapshotId );
        }
        arn =  String.format( "arn:aws:ec2:eucalyptus:%s:snapshot/%s", accountNumber, snapshotId );
      }

      return arn;
    }
  }
}
