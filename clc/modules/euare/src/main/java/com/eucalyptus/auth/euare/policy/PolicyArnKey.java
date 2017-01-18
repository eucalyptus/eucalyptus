/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.auth.euare.policy;

import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName;
import java.util.Set;
import com.eucalyptus.auth.PolicyEvaluationContext;
import com.eucalyptus.auth.PolicyEvaluationWriteContextKey;
import com.eucalyptus.auth.euare.common.policy.IamPolicySpec;
import com.eucalyptus.auth.policy.condition.ArnConditionOp;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.util.TypedKey;
import com.google.common.collect.ImmutableSet;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( PolicyArnKey.KEY_NAME )
public class PolicyArnKey implements EuareKey {
  static final String KEY_NAME = "iam:policyarn";

  private static final TypedKey<String> POLICY_ARN_KEY = TypedKey.create( "Policy" );
  public static final PolicyEvaluationWriteContextKey<String> CONTEXT_KEY =
      PolicyEvaluationWriteContextKey.create( POLICY_ARN_KEY );

  private static final Set<String> actions = ImmutableSet.<String>builder()
      .add( qualifiedName( IamPolicySpec.VENDOR_IAM, IamPolicySpec.IAM_ATTACHGROUPPOLICY ) )
      .add( qualifiedName( IamPolicySpec.VENDOR_IAM, IamPolicySpec.IAM_ATTACHROLEPOLICY ) )
      .add( qualifiedName( IamPolicySpec.VENDOR_IAM, IamPolicySpec.IAM_ATTACHUSERPOLICY ) )
      .add( qualifiedName( IamPolicySpec.VENDOR_IAM, IamPolicySpec.IAM_DETACHGROUPPOLICY ) )
      .add( qualifiedName( IamPolicySpec.VENDOR_IAM, IamPolicySpec.IAM_DETACHROLEPOLICY ) )
      .add( qualifiedName( IamPolicySpec.VENDOR_IAM, IamPolicySpec.IAM_DETACHUSERPOLICY ) )
      .build( );

  @Override
  public String value( ) {
    return PolicyEvaluationContext.get( ).getAttribute( POLICY_ARN_KEY );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !ArnConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". ARN conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) throws JSONException {
    Validation.assertArnValue( value );
  }

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }

}
