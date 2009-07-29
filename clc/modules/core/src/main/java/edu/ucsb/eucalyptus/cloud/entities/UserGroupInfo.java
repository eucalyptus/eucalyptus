package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.util.EucalyptusCloudException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table( name = "user_groups" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class UserGroupInfo {
  @Id
  @GeneratedValue
  @Column( name = "user_group_id" )
  private Long id = -1l;
  @Column( name = "user_group_name" )
  private String name;
  @ManyToMany( cascade = CascadeType.ALL )
  @JoinTable(
      name = "group_has_users",
      joinColumns = { @JoinColumn( name = "user_group_id" ) },
      inverseJoinColumns = @JoinColumn( name = "user_id" )
  )
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  private List<UserInfo> users = new ArrayList<UserInfo>();

  public static UserGroupInfo named( String name ) throws EucalyptusCloudException {
    EntityWrapper<UserGroupInfo> db = new EntityWrapper<UserGroupInfo>();
    UserGroupInfo userGroup = null;
    try {
      userGroup = db.getUnique( new UserGroupInfo( name ) );
    } catch( Exception e ) {
      db.add( new UserGroupInfo( "all" ) );
    }finally {
      db.commit();
    }
    return userGroup;
  }

  public UserGroupInfo() {
  }


  public UserGroupInfo( final String name ) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    UserGroupInfo that = ( UserGroupInfo ) o;

    if ( !name.equals( that.name ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public boolean belongs( UserInfo user ) {
    return this.users.contains( user ) || this.name.equals("all");
  }

  public List<UserInfo> getUsers() {
    return users;
  }

  public void setUsers( final List<UserInfo> users ) {
    this.users = users;
  }
}
