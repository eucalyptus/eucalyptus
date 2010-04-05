package com.eucalyptus.component;

import java.io.Serializable;

public interface ServiceConfiguration extends Serializable {
  public String getName();
  public String getHostName();
  public Integer getPort();
  public String getServicePath();
  public com.eucalyptus.bootstrap.Component getComponent();
  public String getUri( );
}
