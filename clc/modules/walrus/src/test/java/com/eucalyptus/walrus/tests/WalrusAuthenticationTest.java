/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.walrus.tests;

import org.junit.Ignore;
import org.junit.Test;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.walrus.exceptions.AccessDeniedException;
import com.eucalyptus.walrus.pipeline.WalrusAuthenticationHandler;
import com.google.gwt.user.client.Random;


@Ignore("Manual development test")
public class WalrusAuthenticationTest {
	
	private static ChannelBuffer getRandomContent(int size) {
		ChannelBuffer buffer = ChannelBuffers.buffer(size);
		for(int i = 0; i < size; i++) {
			buffer.writeByte((byte)Random.nextInt(Byte.MAX_VALUE));
		}
		
		return buffer;
	}

	@Test
	public static void testWalrusAuthenticationHandler() {
		String bucket = "testbucket";
		String object = "testobject";
		String destURI = StorageProperties.WALRUS_URL + "/" + bucket + "/" + object;
		MappingHttpRequest httpRequest = new MappingHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, destURI);
		httpRequest.setContent(getRandomContent(1024));
		
		//Try the handler
		try {			
			WalrusAuthenticationHandler.EucaAuthentication.authenticate(httpRequest, WalrusAuthenticationHandler.processAuthorizationHeader(httpRequest.getAndRemoveHeader("Authorization")));
		} catch (AccessDeniedException e) {
			e.printStackTrace();
			System.out.println("Failed!");
		}
    }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Running authenticate test");
		testWalrusAuthenticationHandler();
		
	}

}
