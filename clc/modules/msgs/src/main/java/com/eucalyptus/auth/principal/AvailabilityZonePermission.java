package com.eucalyptus.auth.principal;

import java.io.Serializable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.util.HasName;

@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
@DiscriminatorValue( value = "AvailabilityZonePermission" )
public class AvailabilityZonePermission extends BaseAuthorization<HasName> implements Serializable {
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

  @Override
  public boolean check( HasName t ) {
    return this.getValue( ).equals( t.getName( ) ) && "Cluster".equals( t.getClass( ).getSimpleName( ) );
  }

}
