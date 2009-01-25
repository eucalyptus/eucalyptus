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

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.msgs.*;
import junit.framework.TestCase;

public class ImageCacheTest extends TestCase {


    public void testGetImage() throws Throwable {

		Bukkit bukkit = new Bukkit();
        String userId = "admin";
        String bucket = "halothar1221";
        String key = "ttylinux.img.manifest.xml";

        CheckImageType checkImageRequest = new CheckImageType();
        checkImageRequest.setBucket(bucket);
        checkImageRequest.setKey(key);
        checkImageRequest.setUserId(userId);
       // CheckImageResponseType checkImageResponse = bukkit.CheckImage(checkImageRequest);

       // System.out.println(checkImageResponse);

        CacheImageType cacheImageRequest = new CacheImageType();
        cacheImageRequest.setBucket(bucket);
        cacheImageRequest.setKey(key);
        cacheImageRequest.setUserId(userId);
        CacheImageResponseType cacheImageResponse = bukkit.CacheImage(cacheImageRequest);
        System.out.println(cacheImageResponse);

        GetDecryptedImageType getDecryptedImageRequest = new GetDecryptedImageType();
        getDecryptedImageRequest.setBucket(bucket);
        getDecryptedImageRequest.setUserId(userId);
        getDecryptedImageRequest.setKey(key);
//        GetDecryptedImageResponseType getDecryptedImageResponse = bukkit.GetDecryptedImage(getDecryptedImageRequest);
 //       System.out.println(getDecryptedImageResponse);

        FlushCachedImageType flushCachedImageRequest = new FlushCachedImageType();
        flushCachedImageRequest.setBucket(bucket);
        flushCachedImageRequest.setUserId(userId);
        flushCachedImageRequest.setKey(key);
        FlushCachedImageResponseType flushCachedImageResponse = bukkit.FlushCachedImage(flushCachedImageRequest);
        System.out.println(flushCachedImageResponse);

        while(true) {
            Thread.sleep(5000);
        }
    }

    public ImageCacheTest() {
		super();
	}

}