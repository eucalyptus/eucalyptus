package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class WalrusInfoWeb implements IsSerializable {
	private String name;
	private Boolean committed;
    private String bucketsRootDirectory;
    private Integer maxBucketsPerUser;
    private Integer maxBucketSizeInMB;
    private Integer maxCacheSizeInMB;
    private Integer snapshotsTotalInGB;

	public WalrusInfoWeb() {}

	public WalrusInfoWeb( final String name,
            final String bucketsRootDirectory,
            final Integer maxBucketsPerUser,
            final Integer maxBucketSizeInMB,
            final Integer maxCacheSizeInMB,
            final Integer snapshotsTotalInGB) {
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

    public Integer getMaxBucketSizeInMB()
    {
        return maxBucketSizeInMB;
    }

    public void setMaxBucketSizeInMB( final Integer maxBucketSizeInMB )
    {
        this.maxBucketSizeInMB = maxBucketSizeInMB;
    }

    public Integer getMaxBucketsPerUser()
    {
        return maxBucketsPerUser;
    }

    public void setMaxBucketsPerUser( final Integer maxBucketsPerUser )
    {
        this.maxBucketsPerUser = maxBucketsPerUser;
    }

    public Integer getMaxCacheSizeInMB()
    {
        return maxCacheSizeInMB;
    }

    public void setMaxCacheSizeInMB( final Integer maxCacheSizeInMB )
    {
        this.maxCacheSizeInMB = maxCacheSizeInMB;
    }

    public Integer getSnapshotsTotalInGB()
    {
        return snapshotsTotalInGB;
    }

    public void setSnapshotsTotalInGB( final Integer snapshotsTotalInGB )
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
