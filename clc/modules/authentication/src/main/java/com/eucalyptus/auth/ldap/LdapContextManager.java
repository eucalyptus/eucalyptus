package com.eucalyptus.auth.ldap;

import com.eucalyptus.auth.crypto.Hmacs;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import java.util.List;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributesException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * A thin wrapper around LdapContext to provide simpler access and synchronization
 * 
 * @author wenye
 */
public class LdapContextManager {
  
  public static final int           TIMEOUT_IN_MILLIS = 3000;
  
  private static Logger             LOG               = Logger.getLogger( LdapContextManager.class );
  
  private static final boolean      DEBUG             = false;
  
  private static LdapContextManager instance          = null;
  
  public static synchronized LdapContextManager getInstance( ) throws LdapException {
    if ( instance == null ) {
      instance = new LdapContextManager( );
    }
    return instance;
  }
  
  private LdapContext context;
  
  private LdapContextManager( ) throws LdapException {
    Properties env = new Properties( );
    env.put( Context.INITIAL_CONTEXT_FACTORY, LdapConfiguration.LDAP_CONTEXT_FACTORY );
    env.put( Context.PROVIDER_URL, LdapConfiguration.LDAP_LOCAL_SERVER_URL );
    env.put( Context.SECURITY_AUTHENTICATION, LdapConfiguration.LDAP_SECURITY_AUTHENTICATION );
    env.put( Context.SECURITY_PRINCIPAL, LdapConfiguration.LDAP_SECURITY_PRINCIPAL );
    env.put( Context.SECURITY_CREDENTIALS, Hmacs.generateSystemSignature( ) );
    try {
      context = new InitialLdapContext( env, null );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Connection failure", e );
    }
  }
  
  public synchronized void addEntry( String dn, Attributes attrs ) throws EntryExistsException, LdapException {
    if ( DEBUG ) { LOG.debug( "<addEntry> " + dn + ": " + attrs ); }
    try {
      context.bind( dn, null, attrs );
    } catch ( NameAlreadyBoundException e ) {
      LOG.error( e, e );
      throw new EntryExistsException( "Entry already exists", e );
    } catch ( InvalidAttributesException e ) {
      LOG.error( e, e );
      throw new LdapException( "Adding entry with invalid attributes", e );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Can not add an entry " + dn + ": " + attrs, e );
    }
  }
  
  public synchronized void removeEntry( String dn ) throws EntryNotFoundException, LdapException {
    if ( DEBUG ) { LOG.debug( "<removeEntry> " + dn ); }
    try {
      context.destroySubcontext( dn );
    } catch ( NameNotFoundException e ) {
      LOG.error( e, e );
      throw new EntryNotFoundException( "Can not find the entry to remove", e );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Can not remove the entry " + dn, e );
    }
  }
  
  private void collectSearchResult( NamingEnumeration searchReturn, List<Attributes> results ) throws NamingException {
    if ( searchReturn != null ) {
      while ( searchReturn.hasMore( ) ) {
        SearchResult searchResult = ( SearchResult ) searchReturn.next( );
        if ( searchResult != null ) {
          Attributes result = searchResult.getAttributes( );
          if ( result != null ) {
            results.add( result );
            if ( DEBUG ) { LOG.debug( "<search> >> result " + result ); }
          }
        }
      }
    }
  }
  
  public synchronized List<Attributes> search( String baseDn, String filter, String[] attrs ) throws EntryNotFoundException {
    if ( DEBUG ) { LOG.debug( "<search> " + baseDn + ": filter = " + filter ); }
    SearchControls searchControls = new SearchControls( );
    if ( attrs != null ) {
      searchControls.setReturningAttributes( attrs );
    }
    searchControls.setDerefLinkFlag( true );
    searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
    searchControls.setTimeLimit( TIMEOUT_IN_MILLIS );
    
    List<Attributes> results = Lists.newArrayList( );
    try {
      NamingEnumeration searchReturn = context.search( baseDn, filter, searchControls );
      collectSearchResult( searchReturn, results );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new EntryNotFoundException( "Searching " + baseDn + "failed using " + filter, e );
    }
    if ( DEBUG ) { LOG.debug( "<search> >> result END" ); }
    return results;
  }
  
  public synchronized List<Attributes> search( String baseDn, Attributes matchingAttrs, String[] attrs ) throws EntryNotFoundException {
    if ( DEBUG ) { LOG.debug( "<search> " + baseDn + ": " + matchingAttrs ); }
    List<Attributes> results = Lists.newArrayList( );
    try {
      NamingEnumeration searchReturn = context.search( baseDn, matchingAttrs, attrs );
      collectSearchResult( searchReturn, results );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new EntryNotFoundException( "Searching " + baseDn + "failed using " + matchingAttrs, e );
    }
    if ( DEBUG ) { LOG.debug( "<search> >> result END" ); }
    return results;
  }
  
  public synchronized void updateEntry( String dn, Attributes attrs ) throws LdapException {
    if ( DEBUG ) { LOG.debug( "<updateEntry> " + dn + ": " + attrs ); }
    try {
      context.modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, attrs );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Can not update entry " + dn + ": " + attrs, e );
    }
  }
  
  public synchronized void deleteEntryAttribute( String dn, Attributes attrs ) throws LdapException {
    if ( DEBUG ) { LOG.debug( "<deleteEntryAttribute> " + dn + ": " + attrs ); }
    try {
      context.modifyAttributes( dn, DirContext.REMOVE_ATTRIBUTE, attrs );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Can not delete entry attribute " + dn + ": " + attrs, e );
    }
  }
  
  public synchronized void addEntryAttribute( String dn, Attributes attrs ) throws LdapException {
    if ( DEBUG ) { LOG.debug( "<addEntryAttribute> " + dn + ": " + attrs ); }
    try {
      context.modifyAttributes( dn, DirContext.ADD_ATTRIBUTE, attrs );
    } catch ( NamingException e ) {
      LOG.error( e, e );
      throw new LdapException( "Can not add entry attribute " + dn + ": " + attrs, e );
    }
  }
}
