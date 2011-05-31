package com.eucalyptus.reporting.event;

public class S3Event
	implements Event
{
	private final boolean   objectOrBucket;
	private final boolean   createOrDelete;
	private final long      sizeMegs;
	private final String    ownerId;
	private final String    accountId;

	/**
	 * <p>Constructor indicating S3 Object event.
	 *  
	 * @param createOrDelete true if the object is being created, false deleted.
	 * @param sizeMegs the size of the object being created or deleted.
	 */
	public S3Event(boolean createOrDelete, long sizeMegs, String ownerId,
			String accountId)
	{
		super();
		this.objectOrBucket = true;
		this.createOrDelete = createOrDelete;
		this.sizeMegs = sizeMegs;
		this.ownerId = ownerId;
		this.accountId = accountId;
	}
	
	/**
	 * <p>Constructor indicating S3 bucket event.
	 *  
	 * @param createOrDelete true if the bucket is being created, false deleted.
	 */
	public S3Event(boolean createOrDelete, String ownerId,	String accountId)
	{
		super();
		this.objectOrBucket = false;
		this.createOrDelete = createOrDelete;
		this.sizeMegs = 0l;
		this.ownerId = ownerId;
		this.accountId = accountId;
	}

	
	public boolean isObjectOrBucket()
	{
		return objectOrBucket;
	}

	public boolean isCreateOrDelete()
	{
		return createOrDelete;
	}

	public long getSizeMegs()
	{
		return sizeMegs;
	}

	public String getOwnerId()
	{
		return ownerId;
	}

	public String getAccountId()
	{
		return accountId;
	}

	@Override
	public boolean requiresReliableTransmission()
	{
		return true;
	}

}

