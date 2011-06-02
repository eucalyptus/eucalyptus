package com.eucalyptus.webui.server;

public class WebSession {
  
  private String id;
  private String userName;
  private String accountName;
  private long creationTime;
  private long lastAccessTime;
  
  public WebSession( String id, String userName, String accountName, long creationTime, long lastAccessTime ) {
    this.setId( id );
    this.setUserName( userName );
    this.setAccountName( accountName );
    this.setCreationTime( creationTime );
    this.setLastAccessTime( lastAccessTime );
  }

  public void setId( String id ) {
    this.id = id;
  }

  public String getId( ) {
    return id;
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }

  public String getUserName( ) {
    return userName;
  }

  public void setAccountName( String accountName ) {
    this.accountName = accountName;
  }

  public String getAccountName( ) {
    return accountName;
  }

  public void setCreationTime( long creationTime ) {
    this.creationTime = creationTime;
  }

  public long getCreationTime( ) {
    return creationTime;
  }

  public void setLastAccessTime( long lastAccessTime ) {
    this.lastAccessTime = lastAccessTime;
  }

  public long getLastAccessTime( ) {
    return lastAccessTime;
  }
  
  
  
}
