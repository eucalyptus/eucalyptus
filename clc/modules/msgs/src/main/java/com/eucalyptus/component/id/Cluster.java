package com.eucalyptus.component.id;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.component.ComponentId;

public class Cluster extends ComponentId {

  @Override
  public Integer getPort( ) {
    return 8774;
  }

  @Override
  public String getLocalEndpointName( ) {
    return "vm://ClusterEndpoint";
  }

  @Override
  public String getUriPattern( ) {
    return "http://%s:%d/axis2/services/EucalyptusCC";
  }

  @Override
  public Boolean hasDispatcher( ) {
    return false;
  }

  @Override
  public Boolean isAlwaysLocal( ) {
    return false;
  }

  @Override
  public Boolean isCloudLocal( ) {
    return false;
  }
  
}
