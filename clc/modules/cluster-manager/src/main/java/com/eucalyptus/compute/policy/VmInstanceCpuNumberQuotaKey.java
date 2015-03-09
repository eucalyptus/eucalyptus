package com.eucalyptus.compute.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import net.sf.json.JSONException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;

import java.util.Collections;
import java.util.List;

/**
 * Created by ethomas on 3/8/15.
 */
@PolicyKey( VmInstanceCpuNumberQuotaKey.KEY )
public class VmInstanceCpuNumberQuotaKey extends QuotaKey {

  public static final String KEY = "ec2:quota-vminstance.cpu-number";

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
        return Long.toString( RestrictedTypes.usageMetricFunction(CloudMetadata.VmInstanceCpuMetadata.class).apply( AccountFullName.getInstance(id) ) + quantity );
      case Group:
        return NOT_SUPPORTED;
      case User:
        return Long.toString( RestrictedTypes.usageMetricFunction(CloudMetadata.VmInstanceCpuMetadata.class).apply( UserFullName.getInstance(id) ) + quantity );
    }
    throw new AuthException( "Invalid scope" );
  }

  @RestrictedTypes.UsageMetricFunction( CloudMetadata.VmInstanceCpuMetadata.class )
  public enum MeasureCPUs implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName ownerFullName ) {
      return
        measureFromPersistentInstances(ownerFullName) +
          measureFromPendingInstances(ownerFullName);
    }

    private long measureFromPersistentInstances( final OwnerFullName ownerFullName ) {
      long numCpus = 0L;
      try ( TransactionResource tx = Entities.transactionFor( VmInstance.class ) ){
        Criteria criteria = Entities.createCriteria(VmInstance.class)
          .add(Example.create(VmInstance.named(ownerFullName, null)))
          .add(Restrictions.not(Restrictions.in("state", VmInstance.VmStateSet.DONE.array())));
        List<VmInstance> result = (List<VmInstance>) criteria.list();
        if (result != null) {
          for (VmInstance instance : result) {
            numCpus += instance.getVmType().getCpu();
          }
        }
      }
      return numCpus;
    }

    private long measureFromPendingInstances( final OwnerFullName ownerFullName ) {
      long pending = 0;
      for ( final Cluster cluster : Clusters.getInstance().listValues( ) ) {
        pending += cluster.getNodeState( ).measureUncommittedPendingInstanceCpus( ownerFullName );
      }
      return pending;
    }
  }

}
