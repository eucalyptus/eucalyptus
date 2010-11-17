package com.eucalyptus.auth.policy;

import java.util.List;
import java.util.regex.Pattern;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Policies;
import com.eucalyptus.auth.api.PolicyEngine;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.auth.policy.key.CachedKeyEvaluator;
import com.eucalyptus.auth.policy.key.Key;
import com.eucalyptus.auth.policy.key.Keys;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.context.IllegalContextAccessException;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class PolicyEngineImpl implements PolicyEngine {

  public PolicyEngineImpl( ) {
  }
  
  @Override
  public <T> void evaluateAuthorization( Class<T> resourceClass, String resourceName ) throws AuthException {
    try {
      User requestUser = ContextUtils.getRequestUser( );
      Class<? extends BaseMessage> requestMessageClass = ContextUtils.getRequestMessageClass( );
      String userId = requestUser.getUserId( );
      String accountId = requestUser.getAccount( ).getAccountId( );
      String resourceType = getResourceType( resourceClass );
      String action = getAction( requestMessageClass );
      List<Authorization> matchedAuths = lookupMatchedAuthorizations( resourceType, userId, accountId );
      processAuthorizations( matchedAuths, action, resourceName );
    } catch ( AuthException e ) {
      //throw by the policy engine implementation 
      throw e;
    } catch ( IllegalContextAccessException e ) {
      //this would happen if Contexts.lookup() is invoked outside of mule.
      throw new AuthException( "Cannot invoke without a corresponding service context available.", e );
    } catch ( Throwable e ) {
      throw new AuthException( "An error occurred while trying to evaluate policy for resource access", e );
    }
  }
  
  private void processAuthorizations( List<Authorization> authorizations, String action, String resource ) throws AuthException {
    CachedKeyEvaluator keyEval = new CachedKeyEvaluator( );
    for ( Authorization auth : authorizations ) {
      if ( !Pattern.matches( PatternUtils.toJavaPattern( auth.getActionPattern( ) ), action ) ) {
        continue;
      }
      if ( !Pattern.matches( PatternUtils.toJavaPattern( auth.getResourcePattern( ) ), resource ) ) {
        continue;
      }
      if ( !evaluateConditions( auth, keyEval ) ) {
        continue;
      }
      if ( auth.getEffect( ) == EffectType.Deny ) {
        throw new AuthException( AuthException.ACCESS_DENIED );
      } else {
        return;
      }      
    }
    throw new AuthException( AuthException.ACCESS_DENIED );
  }
  
  private boolean evaluateConditions( Authorization auth, CachedKeyEvaluator keyEval ) throws AuthException {
    List<? extends Condition> conditions = auth.getConditions( );
    for ( Condition cond : conditions ) {
      ConditionOp op = Conditions.CONDITION_MAP.get( cond.getType( ) );
      Key key = Keys.KEY_MAP.get( cond.getKey( ) );
      boolean condValue = false;
      for ( String value : cond.getValues( ) ) {
        if ( op.check( keyEval.getValue( key, auth ), value ) ) {
          condValue = true;
          break;
        }
      }
      if ( condValue != true ) {
        return false;
      }
    }
    return true;
  }
  
  private List<Authorization> lookupMatchedAuthorizations( String resourceType, String userId, String accountId ) throws AuthException {
    List<Authorization> results = Lists.newArrayList( );
    results.addAll( Policies.lookupAccountGlobalAuthorizations( resourceType, accountId ) );
    results.addAll( Policies.lookupAccountGlobalAuthorizations( PolicySpecConstants.ALL_RESOURCE, accountId ) );
    results.addAll( Policies.lookupAuthorizations( resourceType, userId ) );
    results.addAll( Policies.lookupAuthorizations( PolicySpecConstants.ALL_RESOURCE, userId ) );
    return results;
  }
  
  private String getAction( Class<? extends BaseMessage> messageClass ) throws AuthException {
    String action = PolicySpecConstants.MESSAGE_CLASS_TO_ACTION.get( messageClass );
    if ( action == null ) {
      throw new AuthException( "The message class does not map to a supported action: " + messageClass.getName( ) );
    }
    return action;
  }
  
  private <T> String getResourceType( Class<T> resourceClass ) throws AuthException {
    String resource = PolicySpecConstants.RESOURCE_CLASS_TO_STRING.get( resourceClass );
    if ( resource == null ) {
      throw new AuthException( "Can not translate resource type: " + resourceClass.getName( ) );
    }
    return resource;
  }

}
