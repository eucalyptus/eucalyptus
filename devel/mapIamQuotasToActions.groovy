/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

