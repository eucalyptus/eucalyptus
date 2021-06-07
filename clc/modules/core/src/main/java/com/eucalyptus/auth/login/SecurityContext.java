/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

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
      if( LoginModule.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) ) && !Modifier.isPrivate( candidate.getModifiers( ) ) ) {
        SecurityContext.registerLoginModule( candidate );
        return true;
      } else {
        return false;        
      }
    }
    
  }

  
}
