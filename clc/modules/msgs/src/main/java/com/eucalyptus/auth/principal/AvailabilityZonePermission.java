package com.eucalyptus.auth.principal;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( value = "AvailabilityZonePermission" )
public class AvailabilityZonePermission extends BaseAuthorization<AvailabilityZonePermission> {
  public AvailabilityZonePermission( ) {}
  
  public AvailabilityZonePermission( String value ) {
    super( value );
  }
  
  public String getDescription( ) {
    return "Grants the ability to run instances in the indicated availability zone";
  }
  
  @Override
  public String getName( ) {
    return "Availability Zone Permission";
  }

  /**
   * @see com.eucalyptus.auth.principal.BaseAuthorization#check(java.lang.Object)
   * @param t
   * @return
   */
  @Override
  public boolean check( AvailabilityZonePermission t ) {
    return this.getValue( ).equals( t.getValue( ) );
  }
  
}
