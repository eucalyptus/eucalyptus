package edu.ucsb.eucalyptus.cloud;

import com.eucalyptus.util.EucalyptusCloudException;

public class FailScriptFailException extends EucalyptusCloudException {

  public FailScriptFailException()
  {
  }

  public FailScriptFailException( final String message )
  {
    super( message );
  }

  public FailScriptFailException( final String message, final Throwable cause )
  {
    super( message, cause );
  }

  public FailScriptFailException( final Throwable cause )
  {
    super( cause );
  }
}
