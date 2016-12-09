/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.simplequeue.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.simplequeue.SimpleQueueMetadata;
import com.eucalyptus.simplequeue.common.policy.SimpleQueuePolicySpec;
import com.eucalyptus.simplequeue.persistence.PersistenceFactory;
import com.eucalyptus.simplequeue.persistence.Queue;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import net.sf.json.JSONException;
@PolicyKey( SimpleQueueNumberQuotaKey.KEY )
public class SimpleQueueNumberQuotaKey extends QuotaKey {

  public static final String KEY = "sqs:quota-queuenumber";

  @Override
  public final void validateValueType( String value ) throws JSONException {
    KeyUtils.validateIntegerValue(value, KEY);
  }

  @Override
  public final boolean canApply( String action ) {
    return PolicySpec.qualifiedName(
        SimpleQueuePolicySpec.VENDOR_SIMPLEQUEUE,
        SimpleQueuePolicySpec.SIMPLEQUEUE_CREATEQUEUE).equals( action );
  }

  @Override
  public final String value( final PolicyScope scope,
                             final String id,
                             final String resource,
                             final Long quantity ) throws AuthException {
    final OwnerFullName name;
    switch ( scope ) {
      case Account:
        name = AccountFullName.getInstance(id);
        break;
      case Group:
        return NOT_SUPPORTED;
      case User:
        return NOT_SUPPORTED;
      default:
        throw new AuthException( "Invalid scope" );
    }
    return Long.toString(
      RestrictedTypes.quantityMetricFunction(SimpleQueueMetadata.QueueMetadata.class).apply( name ) +
        quantity );
  }

  @RestrictedTypes.QuantityMetricFunction( SimpleQueueMetadata.QueueMetadata.class )
  public enum CountQueues implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( final OwnerFullName input ) {
      return PersistenceFactory.getQueuePersistence().countQueues(input.getAccountNumber());
    }
  }
}
