/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.policy;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.key.KeyUtils;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.auth.policy.key.QuotaKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.cloudformation.common.CloudFormationMetadata;
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
