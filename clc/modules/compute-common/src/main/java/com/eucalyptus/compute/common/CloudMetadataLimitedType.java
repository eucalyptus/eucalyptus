package com.eucalyptus.compute.common;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.util.LimitedType;
import com.eucalyptus.util.RestrictedType;

@PolicyVendor( "ec2" )
public interface CloudMetadataLimitedType extends LimitedType {
  @PolicyResourceType( "instance.memory" )
  public interface VmInstanceMemoryMetadata extends CloudMetadataLimitedType {}

  @PolicyResourceType( "instance.cpu" )
  public interface VmInstanceCpuMetadata extends CloudMetadataLimitedType {}

  @PolicyResourceType( "instance.disk" )
  public interface VmInstanceDiskMetadata extends CloudMetadataLimitedType {}


}
