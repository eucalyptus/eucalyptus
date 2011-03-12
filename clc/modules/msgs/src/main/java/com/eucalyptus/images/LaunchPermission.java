package com.eucalyptus.images;

import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_image_launch_permission" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class LaunchPermission extends AbstractPersistent {

  @Column( name = "metadata_image_auth_name" )
  private String accountId;

  @ManyToOne
  @JoinColumn( name = "metadata_image_auth_for_image_id" )
  private ImageInfo parent;
  public LaunchPermission( ) {}
  
  
  public LaunchPermission( ImageInfo parent, String accountId ) {
    super( );
    this.parent = parent;
    this.accountId = accountId;
  }


  public String getAccountId( ) {
    return this.accountId;
  }


  public void setAccountId( String accountId ) {
    this.accountId = accountId;
  }


  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.accountId == null )
      ? 0
      : this.accountId.hashCode( ) );
    return result;
  }


  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( !super.equals( obj ) ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    LaunchPermission other = ( LaunchPermission ) obj;
    if ( this.accountId == null ) {
      if ( other.accountId != null ) {
        return false;
      }
    } else if ( !this.accountId.equals( other.accountId ) ) {
      return false;
    }
    return true;
  }


  public ImageInfo getParent( ) {
    return this.parent;
  }


  public void setParent( ImageInfo parent ) {
    this.parent = parent;
  }


}
