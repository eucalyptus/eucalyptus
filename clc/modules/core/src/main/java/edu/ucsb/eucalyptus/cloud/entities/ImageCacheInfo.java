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
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "ImageCache" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class ImageCacheInfo implements Comparable {
    @Id
    @GeneratedValue
    @Column( name = "image_cache_id" )
    private Long id = -1l;

    @Column( name = "bucket_name" )
    private String bucketName;

    @Column( name = "manifest_name" )
    private String manifestName;

    @Column( name = "image_name" )
    private String imageName;

    @Column( name = "in_cache" )
    private Boolean inCache;

    @Column( name = "size" )
    private Long size;

    @Column( name = "use_count")
    private Integer useCount;


    public ImageCacheInfo() {}

    public ImageCacheInfo(String bucketName, String manifestName) {
        this.bucketName = bucketName;
        this.manifestName = manifestName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getManifestName() {
        return manifestName;
    }

    public void setManifestName(String manifestName) {
        this.manifestName = manifestName;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public Boolean getInCache() {
        return inCache;
    }

    public void setInCache(Boolean inCache) {
        this.inCache = inCache;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
    }

    public int compareTo(Object o) {
        ImageCacheInfo info = (ImageCacheInfo) o;
        if(info.getUseCount() == useCount)
            return 0 ;
        if(info.getUseCount() < useCount)
            return 1;
        else
            return -1;
    }

}
