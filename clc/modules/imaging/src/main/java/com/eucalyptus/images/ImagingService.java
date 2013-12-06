package com.eucalyptus.images;

import org.apache.log4j.Logger;

import com.eucalyptus.util.EucalyptusCloudException;

public class ImagingService {
	private static Logger LOG = Logger.getLogger( ImagingService.class );

	public PutInstanceImportTaskStatusResponseType PutInstanceImportTaskStatus( PutInstanceImportTaskStatusType request ) throws EucalyptusCloudException {
		PutInstanceImportTaskStatusResponseType reply = request.getReply( );
		LOG.info( request );
		return reply;
	}

	public GetInstanceImportTaskResponseType GetInstanceImportTask( GetInstanceImportTaskType request ) {
		GetInstanceImportTaskResponseType reply = request.getReply( );
		LOG.info( request );
		return reply;
	}
}
