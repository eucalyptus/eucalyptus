package com.eucalyptus.auth.callback;

import java.util.Map;
import com.eucalyptus.auth.crypto.Hmac;
import com.eucalyptus.auth.util.SecurityParameter;
import com.eucalyptus.http.MappingHttpRequest;

public class HmacCredentials extends WrappedCredentials<MappingHttpRequest> {
  private Hmac    signatureMethod;
  private Integer signatureVersion;
  private String  queryId;
  private String  signature;
  private String  verb;
  private String  servicePath;
  private String  headerHost;
  private String  headerPort;
  private final Map<String,String> parameters;
  public HmacCredentials( String correlationId, MappingHttpRequest loginData, String signature, Integer signatureVersion, Hmac hmacType ) {
    super( correlationId, loginData );
    this.parameters = loginData.getParameters( );
    this.queryId = this.parameters.get( SecurityParameter.AWSAccessKeyId.toString( ) );
    this.signature = signature;
    this.signatureVersion = signatureVersion;
    this.signatureMethod = hmacType;
    this.verb = this.getLoginData( ).getMethod( ).getName( );
    this.servicePath = this.getLoginData( ).getServicePath( );
    this.headerHost = this.getLoginData( ).getHeader( "Host" );
    this.headerPort = "8773";
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
