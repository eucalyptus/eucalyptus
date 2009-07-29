package edu.ucsb.eucalyptus.cloud.state;
public class DoesNotExistException extends RuntimeException{
  public DoesNotExistException() {
  }

  public DoesNotExistException( final String s ) {
    super( s );
  }
}
