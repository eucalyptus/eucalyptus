package edu.ucsb.eucalyptus.cloud.state;

import javax.persistence.*;
import java.util.*;

@MappedSuperclass
public abstract class AbstractIsomorph {
  private String userName;
  private String uuid;
  private String displayName;
  private Date birthday;
  @Enumerated(EnumType.STRING)
  private State state;

  public AbstractIsomorph( String userName,String displayName ) {
    this.userName = userName;
    this.uuid = UUID.randomUUID().toString();
    this.birthday = new Date();
    this.displayName = displayName;
    this.state = State.NIHIL;
  }

  public abstract Object morph( Object o );

  public String getUserName() {
    return userName;
  }

  public void setUserName( final String userName ) {
    this.userName = userName;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid( final String uuid ) {
    this.uuid = uuid;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName( final String displayName ) {
    this.displayName = displayName;
  }

  public Date getBirthday() {
    return birthday;
  }

  public void setBirthday( final Date birthday ) {
    this.birthday = birthday;
  }

  public State getState() {
    return state;
  }

  public void setState( final State state ) {
    this.state = state;
  }

  public abstract String mapState( );
}
