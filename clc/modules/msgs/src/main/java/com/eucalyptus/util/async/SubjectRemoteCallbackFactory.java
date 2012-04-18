package com.eucalyptus.util.async;


public interface SubjectRemoteCallbackFactory<T extends RemoteCallback, P> extends RemoteCallbackFactory<T> {
  public P getSubject( );
}
