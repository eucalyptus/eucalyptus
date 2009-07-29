package com.eucalyptus.ws;

public class EucalyptusRemoteFault extends Exception {
  String relatesTo;
  String action;
  String faultDetail;
  String faultCode;
  String faultString;

  public EucalyptusRemoteFault( final String action, final String relatesTo, final String faultCode, final String faultString ) {
    super( String.format( "Action:%s Code:%s Id:%s Error: %s", action, faultCode, relatesTo, faultString ) );
    this.relatesTo = relatesTo;
    this.action = action;
    this.faultCode = faultCode;
    this.faultString = faultString;
  }
}
