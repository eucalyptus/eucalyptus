package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.TableDisplay;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.Type;
import com.google.common.collect.Lists;

public class EuareWebBackend {

  private static final Logger LOG = Logger.getLogger( EuareWebBackend.class );

  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String USERS = "users";
  public static final String GROUPS = "groups";
  public static final String POLICIES = "policies";
  
  public static final String ACCOUNT = "account";
  public static final String USER = "user";
  public static final String GROUP = "group";
  
  public static final ArrayList<SearchResultFieldDesc> COMMON_FIELD_DESCS = Lists.newArrayList( );
  static {
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( ID, "ID", false, "20%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, "Name", true, "80%", TableDisplay.MANDATORY, Type.TEXT, true, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( USERS, "Member users", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( GROUPS, "Member groups", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
    COMMON_FIELD_DESCS.add( new SearchResultFieldDesc( POLICIES, "Policies", false, "0px", TableDisplay.NONE, Type.LINK, false, false ) );
  }
  
  public static User getUser( String userName, String accountName ) throws EucalyptusServiceException {
    if ( userName == null || accountName == null ) {
      throw new EucalyptusServiceException( "Empty user name or account name" );
    }
    try {
      Account account = Accounts.lookupAccountByName( accountName );
      User user = account.lookupUserByName( userName );
      return user;
    } catch ( Exception e ) {
      LOG.error( "Failed to verify user " + userName + "@" + accountName );
      throw new EucalyptusServiceException( "Failed to verify user " + userName + "@" + accountName );
    }
  }
  
  public static void checkPassword( User user, String password ) throws EucalyptusServiceException {
    if ( !user.getPassword( ).equals( Crypto.generateHashedPassword( password ) ) ) {
      throw new EucalyptusServiceException( "Incorrect password" );
    }
  }
  
  public static List<SearchResultRow> searchAccounts( String query ) throws EucalyptusServiceException {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      for ( Account account : Accounts.listAllAccounts( ) ) {
        results.add( serializeAccount( account ) );
      }
    } catch ( Exception e ) {
      LOG.error( "Failed to get accounts", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to get accounts" );
    }
    return results;
  }

  private static SearchResultRow serializeAccount( Account account ) throws Exception {
    SearchResultRow result = new SearchResultRow( );
    result.addField( account.getAccountNumber( ) );
    result.addField( account.getName( ) );
    // Search links for account fields: users, groups and policies
    result.addField( QueryBuilder.get( ).start( Categories.USER ).and( ACCOUNT, account.getName( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( Categories.GROUP ).and( ACCOUNT, account.getName( ) ).url( ) );
    result.addField( QueryBuilder.get( ).start( Categories.POLICY ).and( ACCOUNT, account.getName( ) ).url( ) );
    return result;
  }
  
}
