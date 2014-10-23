package com.eucalyptus.cloudformation.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloudformation.CloudFormationMetadata;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import net.sf.json.JSONException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import javax.persistence.EntityTransaction;
import java.util.List;

/**
 * Created by ethomas on 10/22/14.
 */
@PolicyKey( CloudFormationNumberQuotaKey.KEY )
public class CloudFormationNumberQuotaKey extends QuotaKey {

  public static final String KEY = "cloudformation:quota-stacknumber";

  @Override
  public final void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue(value, KEY);
  }

  @Override
  public final boolean canApply( String action, String resourceType ) {
    return PolicySpec.qualifiedName(
      PolicySpec.VENDOR_LOADBALANCING,
      PolicySpec.LOADBALANCING_CREATELOADBALANCER).equals( action );
  }

  @Override
  public final String value( final Authorization.Scope scope,
                             final String id,
                             final String resource,
                             final Long quantity ) throws AuthException {
    final OwnerFullName name;
    switch ( scope ) {
      case ACCOUNT:
        name = AccountFullName.getInstance(id);
        break;
      case GROUP:
        return NOT_SUPPORTED;
      case USER:
        return NOT_SUPPORTED;
      default:
        throw new AuthException( "Invalid scope" );
    }
    return Long.toString(
      RestrictedTypes.quantityMetricFunction(CloudFormationMetadata.StackMetadata.class).apply( name ) +
        quantity );
  }

  @RestrictedTypes.QuantityMetricFunction( CloudFormationMetadata.StackMetadata.class )
  public enum CountStacks implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName input ) {
      try (TransactionResource db =
             Entities.transactionFor(StackEntity.class)) {
        Criteria criteria = Entities.createCriteria(StackEntity.class)
          .add(Restrictions.eq("accountId", input.getAccountNumber()))
          .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
        List<StackEntity> entityList = criteria.list();
        long retVal = entityList == null ? 0L : entityList.size();
        db.rollback();
        return retVal;
      }
    }
  }
}
