package com.eucalyptus.auth.policy.key;

import java.util.Map;
import com.eucalyptus.auth.policy.condition.NumericLessThan;
import com.google.common.collect.Maps;

/**
 * Collect and aggregate quota spec at different levels.
 * 
 * Quota can be specified at account level, group level and user level. The evaluation of the final
 * quota spec is evaluated as follows:
 * 1. At each level, pick the largest quota for a specific key (quota type).
 * 2. When aggregating all levels, pick the smallest quota of all levels for a specific key.
 * 
 * @author wenye
 *
 */
public class QuotaKeyEvaluator {

  public enum Level {
    ACCOUNT,
    GROUP,
    USER,
  }
  
  private Map<Class<? extends QuotaKey>, String> account = Maps.newHashMap( );
  private Map<Class<? extends QuotaKey>, String> group = Maps.newHashMap( );
  private Map<Class<? extends QuotaKey>, String> user = Maps.newHashMap( );
  
  private Map<Level, Map<Class<? extends QuotaKey>, String>> levelMap = Maps.newHashMap( );
  
  public QuotaKeyEvaluator( ) {
    levelMap.put( Level.ACCOUNT, account );
    levelMap.put( Level.GROUP, group );
    levelMap.put( Level.USER, user );
  }

  /**
   * Add quota spec (key, value) to a certain level.
   * 
   * @param level The level of the quota added to.
   * @param keyClass The key type of the quota.
   * @param value The specified value of the quota type.
   */
  public void addLevelQuota( Level level, Class<? extends QuotaKey> keyClass, String value ) {
    addQuotaForLevel( levelMap.get( level ), keyClass, value );
  }
  
  /**
   * @return A map of final quota spec.
   */
  public Map<Class<? extends QuotaKey>, String> getQuotas( ) {
    mergeFromLevelAbove( user, group );
    mergeFromLevelAbove( user, account );
    return user;
  }
  
  // Merge quota of different levels.
  private void mergeFromLevelAbove( Map<Class<? extends QuotaKey>, String> low, Map<Class<? extends QuotaKey>, String> high ) {
    NumericLessThan nlt = new NumericLessThan( );
    for ( Map.Entry<Class<? extends QuotaKey>, String> entry : high.entrySet( ) ) {
      Class<? extends QuotaKey> key = entry.getKey( );
      String highLevelValue = entry.getValue( );
      String lowLevelValue = low.get( key );
      if ( lowLevelValue == null || nlt.check( highLevelValue, lowLevelValue ) ) {
        low.put( key, highLevelValue );
      }
    }
  }
  
  // Add quota to one level.
  private void addQuotaForLevel( Map<Class<? extends QuotaKey>, String> map, Class<? extends QuotaKey> keyClass, String value ) {
    String currentValue = map.get( keyClass );
    NumericLessThan nlt = new NumericLessThan( );
    if ( currentValue == null || nlt.check( currentValue, value ) ) {
      map.put( keyClass, value );
    }
  }
  
}
