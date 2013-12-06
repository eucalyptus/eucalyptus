@GroovyAddClassUUID
package com.eucalyptus.images;
import com.eucalyptus.component.annotation.ComponentMessage

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

@ComponentMessage(Imaging.class)
public class ImagingMessage extends EucalyptusMessage {
  
  public ImagingMessage( ) {
    super( );
  }
  
  public ImagingMessage( EucalyptusMessage msg ) {
    super( msg );
  }
  
  public ImagingMessage( String userId ) {
    super( userId );
  }
}
/** *******************************************************************************/
public class ImportImageType extends ImagingMessage {
  String manifestUrl;

  public ImportImageType( ) {
    super( );
  }

  public ImportImageType( EucalyptusMessage msg ) {
    super( msg );
  }

  public ImportImageType( String manifestUrl ) {
    this.manifestUrl = manifestUrl;
  }
  
}

public class PutInstanceImportTaskStatusResponseType extends ImagingMessage {
}

public class PutInstanceImportTaskStatusType extends ImagingMessage {
}

public class GetInstanceImportTaskResponseType extends ImagingMessage {
}

public class GetInstanceImportTaskType extends ImagingMessage {
}

public class Error extends EucalyptusData {
  String type
  String code
  String message
  public Error() {  }
  ErrorDetail detail = new ErrorDetail()
}

public class ErrorResponse extends ImagingMessage {
  String requestId
  public ErrorResponse() {
  }
  ArrayList<Error> error = new ArrayList<Error>()
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
