package com.eucalyptus.images;

import org.apache.log4j.Logger;

public class ImagingService {
  private static Logger LOG = Logger.getLogger( ImagingService.class );
  public ImportImageResponseType importImage( ImportImageType request ) {
    ImportImageResponseType reply = request.getReply( );
    LOG.info( request );
    return reply;
  }
  
  public ImportVolumeResponseType importVolume( ImportVolumeType request ) {
    ImportVolumeResponseType reply = request.getReply( );
    return reply;
  }
  
  public ImportInstanceResponseType importInstance( ImportInstanceType request ) {
    ImportInstanceResponseType reply = request.getReply( );
    return reply;
  }
  
  public CancelImportTaskResponseType cancelImportTask( CancelImportTaskType request ) {
    CancelImportTaskResponseType reply = request.getReply( );
    return reply;
  }
  
  public DescribeImportTasksResponseType describeImportTasks( DescribeImportTasksType request ) {
    DescribeImportTasksResponseType reply = request.getReply( );
    return reply;
  }
}
