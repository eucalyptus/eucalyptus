/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

import com.eucalyptus.auth.policy.PolicySpec
import com.eucalyptus.auth.policy.key.Keys
import com.eucalyptus.auth.policy.key.QuotaKey

def vendors = [
        PolicySpec.VENDOR_IAM,
        PolicySpec.VENDOR_EC2,
        PolicySpec.VENDOR_S3,
        PolicySpec.VENDOR_STS,
        PolicySpec.VENDOR_AUTOSCALING,
        PolicySpec.VENDOR_CLOUDWATCH,
        PolicySpec.VENDOR_CLOUDFORMATION,
        PolicySpec.VENDOR_LOADBALANCING 
]

def S3_RESOURCES = [
        PolicySpec.S3_RESOURCE_BUCKET,
        PolicySpec.S3_RESOURCE_OBJECT
]

def IAM_RESOURCES = [
        PolicySpec.IAM_RESOURCE_ACCOUNT,
        PolicySpec.IAM_RESOURCE_GROUP,
        PolicySpec.IAM_RESOURCE_INSTANCE_PROFILE,
        PolicySpec.IAM_RESOURCE_ROLE,
        PolicySpec.IAM_RESOURCE_SERVER_CERTIFICATE,
        PolicySpec.IAM_RESOURCE_USER
]

def AUTOSCALING_RESOURCES = [
        PolicySpec.AUTOSCALING_RESOURCE_TAG
]

def resourceTypes = []

resourceTypes += PolicySpec.EC2_RESOURCES
resourceTypes += S3_RESOURCES
resourceTypes += IAM_RESOURCES
resourceTypes += AUTOSCALING_RESOURCES


def output = '\n'
def outputMap = new HashSet<Tuple>()

Keys.KEY_MAP.keySet().each { k ->
  def key = Keys.getKeyInstance(Keys.getKeyClass(k))
  if(key instanceof QuotaKey) {
    vendors.each { vendor ->
            PolicySpec.getActionsForVendor(vendor).asSet().each { vendorActions ->
        vendorActions.each { action ->
                resourceTypes.each { resourceType ->
            def qualifiedResourceType = PolicySpec.qualifiedName(vendor, resourceType)
            def qualifiedAction = PolicySpec.qualifiedName(vendor,action)
                        if(key.canApply(qualifiedAction, qualifiedResourceType)) {
                        //if(key.canApply(qualifiedAction, null)) {
                              //output += 'QuotaKey: ' + k + ' -> ' + qualifiedAction + '\n'
              outputMap.add(new Tuple(k, qualifiedAction))
                        }
                    }
              }
      }
        }
  }
}


outputMap.each { tuple ->
  output += tuple.toString() + '\n'
}
return output

