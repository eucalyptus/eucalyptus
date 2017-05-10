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
