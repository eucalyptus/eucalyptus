package com.eucalyptus.auth.principal.credential;

import java.math.BigInteger;
import com.eucalyptus.auth.principal.BasePrincipal;

public interface CredentialPrincipal extends BasePrincipal {
  /**
   * Returns a unique number associated with the principal.  Not to be confused with the X509Certificate serial number.
   * @return
   */
  public abstract BigInteger getNumber();
}
