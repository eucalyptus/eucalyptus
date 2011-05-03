package com.eucalyptus.webui.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface EuareServiceAsync {
  
  void test( String message, AsyncCallback<String> callback );
  
}
