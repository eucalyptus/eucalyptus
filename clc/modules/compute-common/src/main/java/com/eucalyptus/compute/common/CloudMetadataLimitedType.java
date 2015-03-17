package com.eucalyptus.compute.common;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.util.LimitedType;
import com.eucalyptus.util.RestrictedType;

@PolicyVendor( "ec2" )
public interface CloudMetadataLimitedType extends LimitedType {
  @PolicyResourceType( VmInstanceMemoryMetadata.POLICY_RESOURCE_TYPE )
  public interface VmInstanceMemoryMetadata extends CloudMetadataLimitedType {
    public static String POLICY_RESOURCE_TYPE = "instance-memory";
  }

  @PolicyResourceType( VmInstanceCpuMetadata.POLICY_RESOURCE_TYPE )
  public interface VmInstanceCpuMetadata extends CloudMetadataLimitedType {
    public static String POLICY_RESOURCE_TYPE = "instance-cpu";
  }

  @PolicyResourceType( VmInstanceDiskMetadata.POLICY_RESOURCE_TYPE )
  public interface VmInstanceDiskMetadata extends CloudMetadataLimitedType {
    public static String POLICY_RESOURCE_TYPE = "instance-disk";
  }
}
