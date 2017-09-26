/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.objectstorage.exceptions.s3;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class SignatureDoesNotMatchException extends S3Exception {

  private String accessKeyId;
  private String stringToSign;
  private String signatureProvided;
  private String canonicalRequest;

  public SignatureDoesNotMatchException( ) {
    super( S3ErrorCodeStrings.SignatureDoesNotMatch, "The request signature we calculated does not match the signature you provided.", HttpResponseStatus.FORBIDDEN );
  }

  public SignatureDoesNotMatchException( String resource ) {
    this( );
    this.setResource( resource );
  }

  public SignatureDoesNotMatchException( String accessKeyId, String stringToSign, String signatureProvided ) {
    this( );
    this.accessKeyId = accessKeyId;
    this.stringToSign = stringToSign;
    this.signatureProvided = signatureProvided;
  }

  public SignatureDoesNotMatchException( String accessKeyId, String stringToSign, String signatureProvided, String canonicalRequest ) {
    this( );
    this.accessKeyId = accessKeyId;
    this.stringToSign = stringToSign;
    this.signatureProvided = signatureProvided;
    this.canonicalRequest = canonicalRequest;
  }

  public String getAccessKeyId( ) {
    return this.accessKeyId;
  }

  public String getStringToSign( ) {
    return this.stringToSign;
  }

  public String getSignatureProvided( ) {
    return this.signatureProvided;
  }

  public String getCanonicalRequest( ) {
    return this.canonicalRequest;
  }
}
