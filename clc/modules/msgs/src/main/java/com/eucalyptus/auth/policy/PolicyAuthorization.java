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
package com.eucalyptus.auth.policy;

import static org.hamcrest.Matchers.notNullValue;
import static com.eucalyptus.auth.policy.PolicyUtils.checkParam;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.Principal;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 *
 */
public class PolicyAuthorization implements Authorization {

  private final String statementId;

  private final Authorization.EffectType effect;

  private final String region;

  // The account name or number resource this authorization applies to.
  private final String account;

  // The type of resource this authorization applies to, used to restrict search.
  private final String type;

  private final PolicyPrincipal principal;

  private final List<Condition> conditions;

  // If action list is negated, i.e. NotAction
  private final boolean notAction;

  private final Set<String> actions;

  // If resource list is negated, i.e. NotResource
  private final boolean notResource;

  private final Set<String> resources;

  public PolicyAuthorization(
      @Nullable final String statementId,
      @Nonnull  final Authorization.EffectType effect,
      @Nullable final String region,
      @Nullable final String account,
      @Nullable final String type,
      @Nullable final PolicyPrincipal principal,
      @Nonnull  final List<PolicyCondition> conditions,
      @Nonnull  final Set<String> actions,
                final boolean notAction,
      @Nonnull  final Set<String> resources,
                final boolean notResource
  ) {
    checkParam( "effect", effect, notNullValue() );
    checkParam( "conditions", conditions, notNullValue() );
    checkParam( "actions", actions, notNullValue() );
    checkParam( "resources", resources, notNullValue() );
    this.statementId = PolicyUtils.intern( statementId );
    this.effect = effect;
    this.region = PolicyUtils.intern( region );
    this.account = PolicyUtils.intern( account );
    this.type = PolicyUtils.intern( type );
    this.principal = PolicyUtils.intern( principal );
    this.conditions = ImmutableList.copyOf( Iterables.transform( conditions, PolicyUtils.internCondition() ) );
    this.actions = ImmutableSet.copyOf( Iterables.transform( actions, PolicyUtils.internString( ) ) );
    this.notAction = notAction;
    this.resources = ImmutableSet.copyOf( Iterables.transform( resources, PolicyUtils.internString( ) ) );
    this.notResource = notResource;
  }

  public PolicyAuthorization(
      final String statementId,
      final Authorization.EffectType effect,
      final PolicyPrincipal principal,
      final List<PolicyCondition> conditions,
      final Set<String> actions,
      final boolean notAction
  ) {
    this(
        statementId,
        effect,
        null,
        null,
        null,
        principal,
        conditions,
        actions,
        notAction,
        Collections.<String>emptySet( ),
        false
    );
  }

  public String getStatementId() {
    return statementId;
  }

  public EffectType getEffect() {
    return effect;
  }

  @Override
  public String getRegion() {
    return region;
  }

  public String getAccount() {
    return account;
  }

  public String getType() {
    return type;
  }

  public boolean isNotAction() {
    return notAction;
  }

  public Set<String> getActions() {
    return actions;
  }

  public boolean isNotResource() {
    return notResource;
  }

  public Set<String> getResources() {
    return resources;
  }

  @Override
  public List<Condition> getConditions( ) {
    return conditions;
  }

  @Override
  public Principal getPrincipal( ) {
    return principal;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final PolicyAuthorization that = (PolicyAuthorization) o;

    if ( notAction != that.notAction ) return false;
    if ( notResource != that.notResource ) return false;
    if ( region != null ? !region.equals( that.region ) : that.region != null ) return false;
    if ( account != null ? !account.equals( that.account ) : that.account != null ) return false;
    if ( !actions.equals( that.actions ) ) return false;
    if ( !conditions.equals( that.conditions ) ) return false;
    if ( effect != that.effect ) return false;
    if ( principal != null ? !principal.equals( that.principal ) : that.principal != null ) return false;
    if ( !resources.equals( that.resources ) ) return false;
    if ( statementId != null ? !statementId.equals( that.statementId ) : that.statementId != null ) return false;
    if ( type != null ? !type.equals( that.type ) : that.type != null ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = statementId != null ? statementId.hashCode() : 0;
    result = 31 * result + effect.hashCode();
    result = 31 * result + ( region != null ? region.hashCode() : 0 );
    result = 31 * result + ( account != null ? account.hashCode() : 0 );
    result = 31 * result + ( type != null ? type.hashCode() : 0 );
    result = 31 * result + ( principal != null ? principal.hashCode() : 0 );
    result = 31 * result + conditions.hashCode();
    result = 31 * result + ( notAction ? 1 : 0 );
    result = 31 * result + actions.hashCode();
    result = 31 * result + ( notResource ? 1 : 0 );
    result = 31 * result + resources.hashCode();
    return result;
  }
}
