/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cluster.common.internal.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.compute.common.CloudMetadataLimitedType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.google.common.base.Function;
import net.sf.json.JSONException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by ethomas on 3/8/15.
 */
@PolicyKey( MemoryTotalSizeQuotaKey.KEY )
public class MemoryTotalSizeQuotaKey extends QuotaKey {

  public static final String KEY = "ec2:quota-memorytotalsize";
  public static final String POLICY_RESOURCE_TYPE = CloudMetadataLimitedType.MemoryMetadata.POLICY_RESOURCE_TYPE;
  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue(value, KEY);
  }

  @Override
  public boolean canApply( String action, String resourceType) {
    if ( PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RUNINSTANCES ).equals( action )
      && PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, POLICY_RESOURCE_TYPE).equals(resourceType) ) {
      return true;
    }
    if ( PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, PolicySpec.EC2_STARTINSTANCES ).equals( action )
      && PolicySpec.qualifiedName( PolicySpec.VENDOR_EC2, POLICY_RESOURCE_TYPE).equals(resourceType) ) {
      return true;
    }
    return false;
  }

  @Override
  public String value( PolicyScope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case Account:
        return Long.toString( RestrictedTypes.usageMetricFunction(CloudMetadataLimitedType.MemoryMetadata.class).apply( AccountFullName.getInstance(id) ) + quantity );
      case Group:
        return NOT_SUPPORTED;
      case User:
        return Long.toString( RestrictedTypes.usageMetricFunction(CloudMetadataLimitedType.MemoryMetadata.class).apply( UserFullName.getInstance(id) ) + quantity );
    }
    throw new AuthException( "Invalid scope" );
  }

  @RestrictedTypes.UsageMetricFunction( CloudMetadataLimitedType.MemoryMetadata.class )
  public enum MeasureMemoryAmount implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName ownerFullName ) {
      return
        measureFromPersistentInstances(ownerFullName) +
          measureFromPendingInstances(ownerFullName);
    }

    private long measureFromPersistentInstances( final OwnerFullName ownerFullName ) {
      long numMemorys = 0L;
      try ( TransactionResource tx = Entities.transactionFor( VmInstance.class ) ){
        Criteria criteria = Entities.createCriteria(VmInstance.class)
          .add(Example.create(VmInstance.named(ownerFullName, null)))
          .add(Restrictions.not(Restrictions.in("state", VmInstance.VmStateSet.TORNDOWN.array())));
        List<VmInstance> result = (List<VmInstance>) criteria.list();
        if (result != null) {
          for (VmInstance instance : result) {
            numMemorys += instance.getVmType().getMemory();
          }
        }
      }
      return numMemorys;
    }

    private long measureFromPendingInstances( final OwnerFullName ownerFullName ) {
      long pending = 0;
      for ( final Cluster cluster : Clusters.list( ) ) {
        pending += cluster.getNodeState( ).measureUncommittedPendingInstanceMemoryAmount(ownerFullName);
      }
      return pending;
    }
  }

}
