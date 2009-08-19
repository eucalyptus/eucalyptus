package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class WalrusInfoWeb implements IsSerializable {
	private String name;
	private Boolean committed;
    private String bucketsRootDirectory;
    private Long maxBucketsPerUser;
    private Long maxBucketSizeInMB;
    private Long maxCacheSizeInMB;
    private Long snapshotsTotalInGB;

	public WalrusInfoWeb() {}

	public WalrusInfoWeb( final String name,
            final String bucketsRootDirectory,
            final Long maxBucketsPerUser,
            final Long maxBucketSizeInMB,
            final Long maxCacheSizeInMB,
            final Long snapshotsTotalInGB) {
		this.name = name;
		this.committed = false;
        this.bucketsRootDirectory = bucketsRootDirectory;
        this.maxBucketsPerUser = maxBucketsPerUser;
        this.maxBucketSizeInMB = maxBucketSizeInMB;
        this.maxCacheSizeInMB = maxCacheSizeInMB;
        this.snapshotsTotalInGB = snapshotsTotalInGB;
	}


	public void setCommitted ()
	{
		this.committed = true;
	}

	public Boolean isCommitted ()
	{
		return this.committed;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    public String getBucketsRootDirectory()
    {
        return bucketsRootDirectory;
    }

    public void setBucketsRootDirectory( final String bucketsRootDirectory )
    {
        this.bucketsRootDirectory = bucketsRootDirectory;
    }

    public Long getMaxBucketSizeInMB()
    {
        return maxBucketSizeInMB;
    }

    public void setMaxBucketSizeInMB( final Long maxBucketSizeInMB )
    {
        this.maxBucketSizeInMB = maxBucketSizeInMB;
    }

    public Long getMaxBucketsPerUser()
    {
        return maxBucketsPerUser;
    }

    public void setMaxBucketsPerUser( final Long maxBucketsPerUser )
    {
        this.maxBucketsPerUser = maxBucketsPerUser;
    }

    public Long getMaxCacheSizeInMB()
    {
        return maxCacheSizeInMB;
    }

    public void setMaxCacheSizeInMB( final Long maxCacheSizeInMB )
    {
        this.maxCacheSizeInMB = maxCacheSizeInMB;
    }

    public Long getSnapshotsTotalInGB()
    {
        return snapshotsTotalInGB;
    }

    public void setSnapshotsTotalInGB( final Long snapshotsTotalInGB )
    {
        this.snapshotsTotalInGB = snapshotsTotalInGB;
    }

	@Override
	public boolean equals( final Object o )
	{
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		WalrusInfoWeb that = ( WalrusInfoWeb ) o;

		if ( !name.equals( that.name ) ) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
}
