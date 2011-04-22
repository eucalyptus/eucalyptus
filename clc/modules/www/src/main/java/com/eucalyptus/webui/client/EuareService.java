package com.eucalyptus.webui.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("euare")
public interface EuareService extends RemoteService {
  
  String test( String name );
  
}
