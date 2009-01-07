package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table( name = "image_permissions" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class ImagePermissionInfo {
  @Id
  @GeneratedValue
  @Column( name = "image_permission_id" )
  private Long id = -1l;
  @Column( name = "image_permission_user_name" )
  private String userName;

  public ImagePermissionInfo() {
  }

  public ImagePermissionInfo( final String userName ) {
    this.userName = userName;
  }

  public Long getId() {
    return id;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName( final String userName ) {
    this.userName = userName;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    ImagePermissionInfo that = ( ImagePermissionInfo ) o;

    if ( !userName.equals( that.userName ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return userName.hashCode();
  }
}
