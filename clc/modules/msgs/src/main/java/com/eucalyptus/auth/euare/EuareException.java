package com.eucalyptus.auth.euare;

import com.eucalyptus.util.EucalyptusCloudException;

public class EuareException extends EucalyptusCloudException {

  private static final long serialVersionUID = 1L;

  public static final String ENTITY_ALREADY_EXISTS = "EntityAlreadyExists";
  public static final String LIMIT_EXCEEDED = "LimitExceeded";
  public static final String NO_SUCH_ENTITY = "NoSuchEntity";
  public static final String INTERNAL_FAILURE = "InternalFailure";
  public static final String NOT_AUTHORIZED = "NotAuthorized";
  public static final String DELETE_CONFLICT = "DeleteConflict";
  public static final String NOT_IMPLEMENTED = "NotImplemented";
  public static final String MALFORMED_POLICY_DOCUMENT = "MalformedPolicyDocument";
  public static final String DUPLICATE_CERTIFICATE = "DuplicateCertificate";
  public static final String INVALID_CERTIFICATE = "InvalidCertificate";
  
  private int code;
  private String error;
  
  public EuareException( int code, String error ) {
    super( );
    this.code = code;
    this.error = error;
  }
  
  public EuareException( int code, String error, String message, Throwable cause ) {
    super( message, cause );
    this.code = code;
    this.error = error;
  }
  
  public EuareException( int code, String error, String message ) {
    super( message );
    this.code = code;
    this.error = error;
  }
  
  public EuareException( int code, String error, Throwable cause ) {
    super( cause );
    this.code = code;
    this.error = error;
  }
  
  public int getCode( ) {
    return this.code;
  }
  
  public String getError( ) {
    return this.error;
  }
  
}
