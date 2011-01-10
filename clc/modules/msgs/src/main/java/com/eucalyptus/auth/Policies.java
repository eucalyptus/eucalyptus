package com.eucalyptus.auth;

import java.security.cert.X509Certificate;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.PolicyProvider;
import com.eucalyptus.auth.principal.Authorization;

public class Policies {
  
  private static Logger LOG = Logger.getLogger( Policies.class );
  
  private static PolicyProvider users;

  public static void setPolicyProvider( PolicyProvider provider ) {
    synchronized( Policies.class ) {
      LOG.info( "Setting the user provider to: " + provider.getClass( ) );
      users = provider;
    }
  }
  
  public static PolicyProvider getPolicyProvider() {
     return users;
  }
  
  public static String attachGroupPolicy( String policy, String groupName, String accountName ) throws AuthException, PolicyParseException {
    return Policies.getPolicyProvider( ).attachGroupPolicy( policy, groupName, accountName );
  }
  
  public static String attachUserPolicy( String policy, String userName, String accountName ) throws AuthException, PolicyParseException {
    return Policies.getPolicyProvider( ).attachUserPolicy( policy, userName, accountName );
  }
  
  public static void removeGroupPolicy( String policyId, String groupName, String accountName ) throws AuthException {
    Policies.getPolicyProvider( ).removeGroupPolicy( policyId, groupName, accountName );
  }
  
  public static List<? extends Authorization> lookupAuthorizations( String resourceType, String userId ) throws AuthException {
    return Policies.getPolicyProvider( ).lookupAuthorizations( resourceType, userId );
  }
  
  public static List<? extends Authorization> lookupAccountGlobalAuthorizations( String resourceType, String accountId ) throws AuthException {
    return Policies.getPolicyProvider( ).lookupAccountGlobalAuthorizations( resourceType, accountId );
  }
  
  public static List<? extends Authorization> lookupQuotas( String resourceType, String userId ) throws AuthException {
    return Policies.getPolicyProvider( ).lookupQuotas( resourceType, userId );
  }
  
  public static List<? extends Authorization> lookupAccountGlobalQuotas( String resourceType, String accountId ) throws AuthException {
    return Policies.getPolicyProvider( ).lookupAccountGlobalQuotas( resourceType, accountId );
  }
  
  public static boolean isCertificateActive( X509Certificate cert ) throws AuthException {
    return Policies.getPolicyProvider( ).isCertificateActive( cert );
  }
}
