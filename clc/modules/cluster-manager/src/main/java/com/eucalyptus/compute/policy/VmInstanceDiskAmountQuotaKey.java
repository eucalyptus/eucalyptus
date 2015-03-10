package com.eucalyptus.compute.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.compute.common.CloudMetadataLimitedType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.vm.VmInstance;
import com.google.common.base.Function;
import net.sf.json.JSONException;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by ethomas on 3/8/15.
 */
@PolicyKey( VmInstanceDiskAmountQuotaKey.KEY )
public class VmInstanceDiskAmountQuotaKey extends QuotaKey {

  public static final String KEY = "ec2:quota-vminstance.disk-amount";

  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue(value, KEY);
  }

  @Override
  public boolean canApply( String action ) {
    // requires run-instances
    if ( PolicySpec.qualifiedName(PolicySpec.VENDOR_EC2, PolicySpec.EC2_RUNINSTANCES).equals( action ) ) {
      return true;
    }
    return false;
  }

  @Override
  public String value( PolicyScope scope, String id, String resource, Long quantity ) throws AuthException {
    switch ( scope ) {
      case Account:
        return Long.toString( RestrictedTypes.usageMetricFunction(CloudMetadataLimitedType.VmInstanceDiskMetadata.class).apply( AccountFullName.getInstance(id) ) + quantity );
      case Group:
        return NOT_SUPPORTED;
      case User:
        return Long.toString( RestrictedTypes.usageMetricFunction(CloudMetadataLimitedType.VmInstanceDiskMetadata.class).apply( UserFullName.getInstance(id) ) + quantity );
    }
    throw new AuthException( "Invalid scope" );
  }

  @RestrictedTypes.UsageMetricFunction( CloudMetadataLimitedType.VmInstanceDiskMetadata.class )
  public enum MeasureDiskAmount implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName ownerFullName ) {
      return
        measureFromPersistentInstances(ownerFullName) +
          measureFromPendingInstances(ownerFullName);
    }

    private long measureFromPersistentInstances( final OwnerFullName ownerFullName ) {
      long numDisks = 0L;
      try ( TransactionResource tx = Entities.transactionFor( VmInstance.class ) ){
        Criteria criteria = Entities.createCriteria(VmInstance.class)
          .add(Example.create(VmInstance.named(ownerFullName, null)))
          .add(Restrictions.not(Restrictions.in("state", VmInstance.VmStateSet.DONE.array())));
        List<VmInstance> result = (List<VmInstance>) criteria.list();
        if (result != null) {
          for (VmInstance instance : result) {
            numDisks += instance.getVmType().getDisk();
          }
        }
      }
      return numDisks;
    }

    private long measureFromPendingInstances( final OwnerFullName ownerFullName ) {
      long pending = 0;
      for ( final Cluster cluster : Clusters.getInstance().listValues( ) ) {
        pending += cluster.getNodeState( ).measureUncommittedPendingInstanceDisks(ownerFullName);
      }
      return pending;
    }
  }

}
