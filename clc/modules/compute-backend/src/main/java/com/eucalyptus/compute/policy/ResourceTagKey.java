/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.policy;

import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName;
import static com.eucalyptus.compute.common.policy.ComputePolicySpec.*;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.compute.common.internal.tags.TagCache;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Assert;
import com.google.common.collect.ImmutableSet;
import net.sf.json.JSONException;

/**
 *
 */
public class ResourceTagKey implements ComputeKey {

  private static final Set<String> actions = ImmutableSet.<String>builder( )
      .add( qualifiedName( VENDOR_EC2, EC2_ACCEPTVPCPEERINGCONNECTION ) )
      .add( qualifiedName( VENDOR_EC2, EC2_ASSOCIATEIAMINSTANCEPROFILE ) )
      .add( qualifiedName( VENDOR_EC2, EC2_ATTACHCLASSICLINKVPC ) )
      .add( qualifiedName( VENDOR_EC2, EC2_ATTACHVOLUME ) )
      .add( qualifiedName( VENDOR_EC2, EC2_AUTHORIZESECURITYGROUPEGRESS ) )
      .add( qualifiedName( VENDOR_EC2, EC2_AUTHORIZESECURITYGROUPINGRESS ) )
      .add( qualifiedName( VENDOR_EC2, EC2_CREATETAGS ) )
      .add( qualifiedName( VENDOR_EC2, EC2_CREATEVPCPEERINGCONNECTION ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETEDHCPOPTIONS ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETEINTERNETGATEWAY ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETENETWORKACL ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETENETWORKACLENTRY ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETEROUTE ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETEROUTETABLE ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETESECURITYGROUP ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETETAGS ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETEVOLUME ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DELETEVPCPEERINGCONNECTION ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DETACHCLASSICLINKVPC ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DETACHVOLUME ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DISABLEVPCCLASSICLINK ) )
      .add( qualifiedName( VENDOR_EC2, EC2_DISASSOCIATEIAMINSTANCEPROFILE ) )
      .add( qualifiedName( VENDOR_EC2, EC2_ENABLEVPCCLASSICLINK ) )
      .add( qualifiedName( VENDOR_EC2, EC2_GETCONSOLESCREENSHOT ) )
      .add( qualifiedName( VENDOR_EC2, EC2_REBOOTINSTANCES ) )
      .add( qualifiedName( VENDOR_EC2, EC2_REJECTVPCPEERINGCONNECTION ) )
      .add( qualifiedName( VENDOR_EC2, EC2_REPLACEIAMINSTANCEPROFILEASSOCIATION ) )
      .add( qualifiedName( VENDOR_EC2, EC2_REVOKESECURITYGROUPEGRESS ) )
      .add( qualifiedName( VENDOR_EC2, EC2_REVOKESECURITYGROUPINGRESS ) )
      .add( qualifiedName( VENDOR_EC2, EC2_RUNINSTANCES ) )
      .add( qualifiedName( VENDOR_EC2, EC2_STARTINSTANCES ) )
      .add( qualifiedName( VENDOR_EC2, EC2_STOPINSTANCES ) )
      .add( qualifiedName( VENDOR_EC2, EC2_TERMINATEINSTANCES ) )
      .build( );

  @Nonnull private final String name;
  @Nonnull private final String tagKey;

  public ResourceTagKey( @Nonnull final String name, @Nonnull final String tagKey ) {
    this.name = Assert.notNull( name, "name" );
    this.tagKey = Assert.notNull( tagKey, "tagKey" );
  }

  @Override
  public String name( ) {
    return name;
  }

  @Override
  public String value( ) throws AuthException {
    String value = null;
    final String resourceId = ComputePolicyContext.getResourceId( );
    if ( resourceId != null ) {
      final Context context = Contexts.lookup( );
      final String accountNumber = context.getAccountNumber( );
      final Map<String,String> tags =
          TagCache.getInstance( ).getTagsForResource( resourceId, accountNumber );
      value = tags.get( tagKey );
    }
    return value;
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( name( ) + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }
}
