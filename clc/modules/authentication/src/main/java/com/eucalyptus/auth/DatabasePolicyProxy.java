package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.entities.Transactions;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

public class DatabasePolicyProxy implements Policy {

  private static final long serialVersionUID = 1L;
  
  private static Logger LOG = Logger.getLogger( DatabasePolicyProxy.class );
                                               
  private PolicyEntity delegate;
  
  public DatabasePolicyProxy( PolicyEntity delegate ) {
    this.delegate = delegate;
  }
  
  @Override
  public String getPolicyId( ) {
    return this.delegate.getPolicyId( );
  }
  
  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }
  
  @Override
  public String getText( ) {
    return this.delegate.getText( );
  }
  
  @Override
  public String getVersion( ) {
    return this.delegate.getPolicyVersion( );
  }
  
  @Override
  public Group getGroup( ) throws AuthException {
    final List<Group> results = Lists.newArrayList( );
    try {
      Transactions.one( PolicyEntity.newInstanceWithId( this.delegate.getPolicyId( ) ), new Tx<PolicyEntity>( ) {
        public void fire( PolicyEntity t ) {
          results.add( new DatabaseGroupProxy( t.getGroup( ) ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setName for " + this.delegate );
      throw new AuthException( e );
    }
    return results.get( 0 );
  }
  
}
