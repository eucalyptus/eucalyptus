package com.eucalyptus.upgrade;

import java.lang.reflect.Modifier;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

public class UpgradeScriptDiscovery extends ServiceJarDiscovery {
  
  @Override
  public Double getPriority( ) {
    return 0.92d;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Throwable {
    if( UpgradeScript.class.isAssignableFrom( candidate ) && !Modifier.isInterface( candidate.getModifiers( ) ) && !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
      StandalonePersistence.registerUpgradeScript( ( UpgradeScript ) candidate.newInstance( ) );
      return true;
    } else {
      return false;
    }
  }
  
}
