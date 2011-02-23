package com.eucalyptus.cloud;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.HasOwningAccount;

@PolicyResourceType( vendor = PolicySpec.VENDOR_EC2, resource = PolicySpec.EC2_RESOURCE_IMAGE )
public interface Image extends HasFullName<Image>, HasOwningAccount {
    
  
}