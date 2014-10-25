package com.eucalyptus.cloudformation;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.util.RestrictedType;

/**
 * Created by ethomas on 10/22/14.
 */
@PolicyVendor( PolicySpec.VENDOR_CLOUDFORMATION )
public interface CloudFormationMetadata extends RestrictedType {
  @PolicyResourceType("stack")
  public interface StackMetadata extends CloudFormationMetadata {}

}
