package edu.ucsb.eucalyptus.cloud;

/**
 * Fix for EUCA-2782. Exception type to be used by HEAD requests when the specified bucket does not exist
 * 
 * @author Swathi Gangisetty
 * 
 */
@SuppressWarnings("serial")
public class HeadNoSuchBucketException extends NoSuchBucketException implements HeadExceptionInterface {

	public HeadNoSuchBucketException() {
		super();
	}

	public HeadNoSuchBucketException(String message, Throwable ex) {
		super(message, ex);
	}

	public HeadNoSuchBucketException(String bucket) {
		super(bucket);
	}

	public HeadNoSuchBucketException(Throwable ex) {
		super(ex);
	}
}
