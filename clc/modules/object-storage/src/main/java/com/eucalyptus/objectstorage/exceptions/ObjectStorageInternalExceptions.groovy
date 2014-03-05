package com.eucalyptus.objectstorage.exceptions

class ObjectStorageInternalException extends RuntimeException {
	public ObjectStorageInternalException() {}
	
	public ObjectStorageInternalException(String msg) {
		super(msg);
	}
	
	public ObjectStorageInternalException(Throwable cause) {
		super(cause);
	}
	
	public ObjectStorageInternalException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

/**
 * A metadata operation could not be completed. Either the db failed
 * or something prevented the update from committing. This is not a conflict or
 * state exception
 */
class MetadataOperationFailureException extends ObjectStorageInternalException {
	public MetadataOperationFailureException() {}
	
	public MetadataOperationFailureException(String msg) {
		super(msg);
	}
	
	public MetadataOperationFailureException(Throwable cause) {
		super(cause);
	}
	
	public MetadataOperationFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

class NoSuchEntityException extends ObjectStorageInternalException {
    public NoSuchEntityException() {}

    public NoSuchEntityException(String msg) {
        super(msg);
    }

    public NoSuchEntityException(Throwable cause) {
        super(cause);
    }

    public NoSuchEntityException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

/**
 * The current state of the resource is not compatible with the requested
 * update. Either a state-machine does not allow the transition or some state
 * of the entity prohibits an update
 */
class IllegalResourceStateException extends ObjectStorageInternalException {
	String expected;
	String found;
	
	public IllegalResourceStateException() {}
	
	public IllegalResourceStateException(String msg) {
		super(msg);
	}

    public IllegalResourceStateException(String msg, Throwable cause) {
        super(msg, cause);
    }
	
	public IllegalResourceStateException(String msg, Throwable cause, String expectedState, String foundState) {
        super(msg, cause);
		expected = expectedState;
		found = foundState;
	}	
}
