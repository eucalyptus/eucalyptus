package edu.ucsb.eucalyptus.cloud;

/**
 * Fix for EUCA-2782. Exception type to be used by HEAD requests when the specified entity does not exist
 * 
 * @author Swathi Gangisetty
 * 
 */
@SuppressWarnings("serial")
public class HeadNoSuchEntityException extends NoSuchEntityException implements HeadExceptionInterface {

	public HeadNoSuchEntityException() {
		super();
	}

	public HeadNoSuchEntityException(String entityName, BucketLogData logData) {
		super(entityName, logData);
	}

	public HeadNoSuchEntityException(String message, Throwable ex) {
		super(message, ex);
	}

	public HeadNoSuchEntityException(String entityName) {
		super(entityName);
	}

	public HeadNoSuchEntityException(Throwable ex) {
		super(ex);
	}
}
