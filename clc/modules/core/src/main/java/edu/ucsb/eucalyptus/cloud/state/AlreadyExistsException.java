package edu.ucsb.eucalyptus.cloud.state;
public class AlreadyExistsException extends RuntimeException {
  public AlreadyExistsException() {
  }

  public AlreadyExistsException( final String s ) {
    super( s );
  }
}
