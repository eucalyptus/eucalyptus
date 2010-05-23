package com.eucalyptus.auth.login;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.spi.LoginModule;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.util.TimedEvictionSet;
import com.google.common.collect.Lists;

public class SecurityContext extends Configuration {
  private static SecurityContext singleton = new SecurityContext( );
  private static TimedEvictionSet<String> replayQueue = new TimedEvictionSet<String>( 10*1000l );
  private List<String> loginModules = Lists.newArrayList( );
  private SecurityContext( ) {}
  
  public static void enqueueSignature( String signature ) throws AuthenticationException {
    if( !SecurityContext.replayQueue.add( signature ) ) {
      throw new AuthenticationException( "Message replay detected.  Same signature was used within the last 15 minutes." );
    }
  }
  
  public static LoginContext getLoginContext( WrappedCredentials credentials ) throws LoginException {
    return new LoginContext( "eucalyptus" , new Subject( ), credentials, singleton );
  }

  
  
  public static void registerLoginModule( Class loginModuleClass ) {
    singleton.loginModules.add( loginModuleClass.getName( ) );
  }
  
  @SuppressWarnings( "unchecked" )
  private static Map emptyMap = new HashMap( );
  
  @Override
  public AppConfigurationEntry[] getAppConfigurationEntry( String name ) {
    AppConfigurationEntry[] entries = new AppConfigurationEntry[loginModules.size( )];
    for( int i = 0; i < entries.length; i++ ) {
      entries[i] = new AppConfigurationEntry( loginModules.get( i ), LoginModuleControlFlag.SUFFICIENT, emptyMap );
    }
    return entries;
  }
  
  public static class LoginModuleDiscovery extends ServiceJarDiscovery {

    @Override
    public Double getPriority( ) {
      return 0.2d;
    }

    @Override
    public boolean processClass( Class candidate ) throws Throwable {
      if( LoginModule.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
        SecurityContext.registerLoginModule( candidate );
        return true;
      } else {
        return false;        
      }
    }
    
  }

  
}
