/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.compute.policy;

import javax.annotation.Nullable;
import com.eucalyptus.blockstorage.Snapshots;
import com.eucalyptus.records.Logs;

/**
 *
 */
public class ComputePolicyContext {
  private final static ThreadLocal<ComputePolicyContextResource> resourceLocal = new ThreadLocal<>();

  static void clearContext( ) {
    resourceLocal.set( null );
  }

  static void setComputePolicyContextResource( @Nullable final ComputePolicyContextResource resource ) {
    resourceLocal.set( resource );
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
  static String getInstanceProfileArn( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getInstanceProfileArn();
  }

  @Nullable
  static String getInstanceType( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getInstanceType( );
  }

  @Nullable
  static String getParentSnapshotArn( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getParentSnapshotArn( );
  }

  @Nullable
  static String getPlacementGroupArn( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getPlacementGroupArn();
  }

  @Nullable
  static String getRegion( ) {
    return "eucalyptus";
  }

  @Nullable
  static String getRootDeviceType( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getRootDeviceType();
  }

  @Nullable
  static String getTenancy( ) {
    final ComputePolicyContextResource resource = resourceLocal.get( );
    return resource == null ? null : resource.getTenancy();
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

  public static interface ComputePolicyContextResource {
    @Nullable
    String getAvailabilityZone( );

    @Nullable
    Boolean isEbsOptimized( );

    @Nullable
    String getInstanceProfileArn( );

    @Nullable
    String getInstanceType( );

    @Nullable
    String getParentSnapshotArn( );

    @Nullable
    String getPlacementGroupArn( );

    @Nullable
    String getRootDeviceType( );

    @Nullable
    String getTenancy( );

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
    public String getInstanceProfileArn( ) {
      return null;
    }

    @Override
    @Nullable
    public String getInstanceType( ) {
      return null;
    }

    @Override
    @Nullable
    public String getParentSnapshotArn( ) {
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

    @Override
    @Nullable
    public String getTenancy( ) {
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
