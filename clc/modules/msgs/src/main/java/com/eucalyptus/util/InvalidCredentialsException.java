package com.eucalyptus.util;

import com.eucalyptus.util.async.FailedRequestException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class InvalidCredentialsException extends FailedRequestException {

  public InvalidCredentialsException( BaseMessage msg ) {
    super( msg );
  }

  public InvalidCredentialsException( String message, BaseMessage msg ) {
    super( message, msg );
  }

  public InvalidCredentialsException( String message, Throwable ex, BaseMessage msg ) {
    super( message, ex, msg );
  }

  public InvalidCredentialsException( Throwable ex, BaseMessage msg ) {
    super( ex, msg );
  }


}
