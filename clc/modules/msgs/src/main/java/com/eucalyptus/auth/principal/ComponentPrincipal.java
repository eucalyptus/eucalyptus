package com.eucalyptus.auth.principal;

import java.net.URL;
import com.eucalyptus.auth.principal.credential.HmacPrincipal;
import com.eucalyptus.auth.principal.credential.X509Principal;

public interface ComponentPrincipal extends BasePrincipal, X509Principal, HmacPrincipal {
  public URL getAddress( );
}
