/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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

    @Column( name = "is_caching" )
    private Boolean isCaching;


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

    public Boolean getCaching() {
        return isCaching;
    }

    public void setCaching(Boolean caching) {
        isCaching = caching;
    }
}