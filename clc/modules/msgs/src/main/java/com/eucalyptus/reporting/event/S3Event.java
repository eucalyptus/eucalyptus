package com.eucalyptus.reporting.event;

public class S3Event
	implements Event
{
	private final boolean   objectOrBucket;
	private final boolean   createOrDelete;
	private final long      sizeMegs;
	private final String    ownerId;
	private final String    ownerName;
	private final String    accountId;
	private final String    accountName;

	/**
	 * <p>Constructor indicating S3 Object event.
     *
     * NOTE: We must include separate userId, username, accountId, and
     *  accountName with each event sent, even though the names can be looked
     *  up using ID's. We must include this redundant information, for
     *  several reasons. First, the reporting subsystem may run on a totally
     *  separate machine outside of eucalyptus (data warehouse configuration)
     *  so it may not have access to the regular eucalyptus database to lookup
     *  usernames or account names. Second, the reporting subsystem stores
     *  <b>historical</b> information, and its possible that usernames and
     *  account names can change, or their users or accounts can be deleted.
     *  Thus we need the user name or account name at the time an event was
     *  sent.
	 *  
	 * @param createOrDelete true if the object is being created, false deleted.
	 * @param sizeMegs the size of the object being created or deleted.
     */
	public S3Event(boolean createOrDelete, long sizeMegs, String ownerId,
			String ownerName, String accountId, String accountName)
	{
		super();
		if (sizeMegs < 0) throw new IllegalArgumentException("sizeMegs for s3 objects cannot be negative");
		if (ownerId==null) throw new IllegalArgumentException("ownerId cant be null");
		if (accountId==null) throw new IllegalArgumentException("ownerId cant be null");

		this.objectOrBucket = true;
		this.createOrDelete = createOrDelete;
		this.sizeMegs = sizeMegs;
		this.ownerId = ownerId;
		this.ownerName = ownerName;
		this.accountId = accountId;
		this.accountName = accountName;
	}
	
	/**
	 * <p>Constructor indicating S3 bucket event.
     *
     * NOTE: We must include separate userId, username, accountId, and
     *  accountName with each event sent, even though the names can be looked
     *  up using ID's. We must include this redundant information, for
     *  several reasons. First, the reporting subsystem may run on a totally
     *  separate machine outside of eucalyptus (data warehouse configuration)
     *  so it may not have access to the regular eucalyptus database to lookup
     *  usernames or account names. Second, the reporting subsystem stores
     *  <b>historical</b> information, and its possible that usernames and
     *  account names can change, or their users or accounts can be deleted.
     *  Thus we need the user name or account name at the time an event was
     *  sent.
	 *  
	 * @param createOrDelete true if the bucket is being created, false deleted.
     */
	public S3Event(boolean createOrDelete, String ownerId,	String ownerName,
			String accountId, String accountName)
	{
		super();
		if (ownerId==null) throw new IllegalArgumentException("ownerId cant be null");
		if (accountId==null) throw new IllegalArgumentException("ownerId cant be null");

		this.objectOrBucket = false;
		this.createOrDelete = createOrDelete;
		this.sizeMegs = 0l;
		this.ownerId = ownerId;
		this.ownerName = ownerName;
		this.accountId = accountId;
		this.accountName = accountName;
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
	
	public String getOwnerName()
	{
		return ownerName;
	}

	public String getAccountId()
	{
		return accountId;
	}
	
	public String getAccountName()
	{
		return accountName;
	}

	@Override
	public boolean requiresReliableTransmission()
	{
		return true;
	}

}

