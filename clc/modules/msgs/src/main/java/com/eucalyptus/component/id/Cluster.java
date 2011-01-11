package com.eucalyptus.component.id;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.component.ComponentIdentity;

public class Cluster extends ComponentIdentity {

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
  
}
