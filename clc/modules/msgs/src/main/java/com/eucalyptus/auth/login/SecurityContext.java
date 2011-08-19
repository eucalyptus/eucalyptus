package com.eucalyptus.auth.login;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.spi.LoginModule;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.util.TimedEvictionSet;
import com.google.common.collect.Lists;
import com.eucalyptus.ws.StackConfiguration;

public class SecurityContext extends Configuration {
  private static SecurityContext singleton = new SecurityContext( );
  private static Logger LOG = Logger.getLogger( SecurityContext.class );
  // Note: According WS-Security spec, WS-Security requests need 
  // to be cached for at least 5 min for timestamps to expire
  // For AWS query interface, default expiration time is 15 mins
  // we cache for 15 mins 20 secs to allow for some clock drift
  // (in case creation Timestamp is up to 20 secs in the future)
  private static TimedEvictionSet<String> replayQueue = new TimedEvictionSet<String>(TimeUnit.MILLISECONDS.convert(900 + StackConfiguration.CLOCK_SKEW_SEC, TimeUnit.SECONDS));
  private List<String> loginModules = Lists.newArrayList( );
  private SecurityContext( ) {}
  
  public static void enqueueSignature( String signature ) throws AuthenticationException {
    if( !SecurityContext.replayQueue.add( signature ) ) {
    	LOG.info("Replay detected for " + signature);
    	throw new AuthenticationException( "Message replay detected.  Same signature was used within the last 15 minutes");
    }
  }
  
  public static LoginContext getLoginContext( WrappedCredentials credentials ) throws LoginException {
    return new LoginContext( "eucalyptus" , new Subject( ), credentials, singleton );
  }

  /**
   * Makes sure that the difference between the expiration time
   * does not exceed replay detection caching
   * 
   * @param createdMillis
   * @param expiresMillis
   * @return
   */
  public static boolean validateTimestampPeriod(Date expires) {
	 Long nanoLimit = replayQueue.getEvictionNanos();
	 
	 Date currentDate = new Date();
	 if((currentDate.getTime() + (nanoLimit / 1000000)) < expires.getTime())
		 return false;
	 
	  return true;
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
    public boolean processClass( Class candidate ) throws Exception {
      if( LoginModule.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
        SecurityContext.registerLoginModule( candidate );
        return true;
      } else {
        return false;        
      }
    }
    
  }

  
}
