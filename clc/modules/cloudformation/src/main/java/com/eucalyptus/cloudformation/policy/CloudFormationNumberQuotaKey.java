/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.cloudformation.CloudFormationMetadata;
import com.eucalyptus.cloudformation.common.policy.CloudFormationPolicySpec;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import net.sf.json.JSONException;

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
  public final boolean canApply( String action ) {
    return PolicySpec.qualifiedName(
        CloudFormationPolicySpec.VENDOR_CLOUDFORMATION,
        CloudFormationPolicySpec.CLOUDFORMATION_CREATESTACK).equals( action );
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
        long retVal = Entities.count(StackEntity.exampleUndeletedWithAccount(input.getAccountNumber()));
        db.rollback();
        return retVal;
      }
    }
  }
}
