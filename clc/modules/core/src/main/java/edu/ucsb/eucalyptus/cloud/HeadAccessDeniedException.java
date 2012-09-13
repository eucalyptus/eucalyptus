package edu.ucsb.eucalyptus.cloud;

/**
 * Fix for EUCA-2782. Exception type to be used by HEAD requests when access to the specified entity is forbidden
 * 
 * @author Swathi Gangisetty
 * 
 */
@SuppressWarnings("serial")
public class HeadAccessDeniedException extends AccessDeniedException implements HeadExceptionInterface {

	public HeadAccessDeniedException() {
		super();
	}

	public HeadAccessDeniedException(String entityType, String entity, BucketLogData logData) {
		super(entityType, entity, logData);
	}

	public HeadAccessDeniedException(String entityType, String entity) {
		super(entityType, entity);
	}

	public HeadAccessDeniedException(String message, Throwable ex) {
		super(message, ex);
	}

	public HeadAccessDeniedException(String entity) {
		super(entity);
	}

	public HeadAccessDeniedException(Throwable ex) {
		super(ex);
	}
}
