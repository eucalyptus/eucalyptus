/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
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
