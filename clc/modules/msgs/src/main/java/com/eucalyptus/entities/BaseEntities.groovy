package com.eucalyptus.entities;

import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.FetchType;
import javax.persistence.CascadeType;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;
import javax.persistence.Version;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import org.hibernate.annotations.GenericGenerator;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.msgs.PacketFilterRule;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.principal.FakePrincipals;


@MappedSuperclass
public class AbstractPersistent implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "id" )
  String id;
  @Version
  @Column(name = "version")
  Integer version = 0;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_update_timestamp")
  Date lastUpdate;
  
  public AbstractPersistent( ) {
    super( );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( id == null ) ? 0 : id.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( obj == null ) return false;
    if ( !getClass( ).is( obj.getClass( ) ) ) return false;
    AbstractPersistent other = ( AbstractPersistent ) obj;
    if ( id == null ) {
      if ( other.id != null ) return false;
    } else if ( !id.equals( other.id ) ) return false;
    return true;
  }
  
}

@MappedSuperclass
public abstract class AccountMetadata extends AbstractPersistent implements Serializable {
  @Column( name = "metadata_account_id" )
  String accountId;
  @Column( name = "metadata_display_name" )
  String displayName;
  public AccountMetadata() {}
  public AccountMetadata(AccountFullName account) {
    this.accountId = account.getAccountId( );
  }
  public AccountMetadata(AccountFullName account, String displayName) {
    this.accountId = account.getAccountId( );
    this.displayName = displayName;
  }
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( accountId == null ) ? 0 : accountId.hashCode( ) );
    result = prime * result + ( ( displayName == null ) ? 0 : displayName.hashCode( ) );
    return result;
  }
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( !super.equals( obj ) ) return false;
    if ( !getClass( ).is( obj.getClass( ) ) ) return false;
    AccountMetadata other = ( AccountMetadata ) obj;
    if ( accountId == null ) {
      if ( other.accountId != null ) return false;
    } else if ( !accountId.equals( other.accountId ) ) return false;
    if ( displayName == null ) {
      if ( other.displayName != null ) return false;
    } else if ( !displayName.equals( other.displayName ) ) return false;
    return true;
  }

}
@MappedSuperclass
public abstract class UserMetadata extends AccountMetadata implements Serializable {
  @Column( name = "metadata_user_id" )
  String userId;
  public UserMetadata( ) {
  }
  public UserMetadata( UserFullName user ) {
    super( user );
    this.userId = user.getUniqueId( );
  }
  public UserMetadata( UserFullName user, String displayName ) {
    super( user, displayName );
    this.userId = user.getUniqueId( );
  }  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( userId == null ) ? 0 : userId.hashCode( ) );
    return result;
  }
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( !super.equals( obj ) ) return false;
    if ( !getClass( ).is( obj.getClass( ) ) ) return false;
    UserMetadata other = ( UserMetadata ) obj;
    if ( userId == null ) {
      if ( other.userId != null ) return false;
    } else if ( !userId.equals( other.userId ) ) return false;
    return true;
  }  
}



