package com.eucalyptus.webui.server;

public class WebSession {
  
  private String id;
  private String userId;
  private long creationTime;
  private long lastAccessTime;
  
  public WebSession( String id, String userId, long creationTime, long lastAccessTime ) {
    this.setId( id );
    this.setUserId( userId );
    this.setCreationTime( creationTime );
    this.setLastAccessTime( lastAccessTime );
  }

  public void setId( String id ) {
    this.id = id;
  }

  public String getId( ) {
    return id;
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

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }
  
}
