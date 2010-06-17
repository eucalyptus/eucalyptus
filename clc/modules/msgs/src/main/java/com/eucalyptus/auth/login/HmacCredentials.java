package com.eucalyptus.auth.login;

import java.util.Map;
import com.eucalyptus.auth.crypto.Hmac;
import com.eucalyptus.auth.util.SecurityParameter;

public class HmacCredentials extends WrappedCredentials<String> {
  private Hmac    signatureMethod;
  private Integer signatureVersion;
  private String  queryId;
  private String  signature;
  private String  verb;
  private String  servicePath;
  private String  headerHost;
  private String  headerPort;
  private final Map<String,String> parameters;
  public HmacCredentials( String correlationId, String signature, Map<String,String> parameters, String verb, String servicePath, String headerHost, Integer signatureVersion, Hmac hmacType ) {
    super( correlationId, signature );
    this.parameters = parameters;
    this.queryId = this.parameters.get( SecurityParameter.AWSAccessKeyId.toString( ) );
    this.signature = signature;
    this.signatureVersion = signatureVersion;
    this.signatureMethod = hmacType;
    this.verb = verb;
    this.servicePath = servicePath;
    this.headerHost = headerHost;
    this.headerPort = System.getProperty("euca.ws.port");
    if ( headerHost != null && headerHost.contains( ":" ) ) {
      String[] hostTokens = this.headerHost.split( ":" );
      this.headerHost = hostTokens[0];
      if ( hostTokens.length > 1 && hostTokens[1] != null && !"".equals( hostTokens[1] ) ) {
        this.headerPort = hostTokens[1];
      }
    }
  }
    
  public Integer getSignatureVersion( ) {
    return this.signatureVersion;
  }
  
  public String getQueryId( ) {
    return this.queryId;
  }

  public String getSignature( ) {
    return this.signature;
  }

  public String getVerb( ) {
    return this.verb;
  }

  public String getServicePath( ) {
    return this.servicePath;
  }

  public String getHeaderHost( ) {
    return this.headerHost;
  }

  public String getHeaderPort( ) {
    return this.headerPort;
  }

  public Hmac getSignatureMethod( ) {
    return this.signatureMethod;
  }

  public Map<String, String> getParameters( ) {
    return this.parameters;
  }
  
}
