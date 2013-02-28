package edu.ucsb.eucalyptus.cloud.ws.tests;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Assert;
import org.junit.Test;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.ws.handlers.WalrusAuthenticationHandler;
import com.google.gwt.user.client.Random;

import edu.ucsb.eucalyptus.cloud.ws.HttpReader;
import edu.ucsb.eucalyptus.cloud.ws.HttpWriter;
import edu.ucsb.eucalyptus.util.WalrusDataMessage;

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
		} catch (AuthenticationException e) {
			e.printStackTrace();
			System.out.println("Failed!");
		}
	}
	
	@Test
	public static void testWriter() {
		String bucket = "testbucket";
		String key = "key";
		String eucaOperation = null;
		String eucaHeader = null;
		HttpWriter writer = new HttpWriter("PUT", bucket, key, eucaOperation, eucaHeader);
		try {
			writer.run();
		} catch (EucalyptusCloudException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public static void testReader() {
		LinkedBlockingQueue<WalrusDataMessage> queue = new LinkedBlockingQueue<WalrusDataMessage>();
		File outputFile = null;
		String eucaOperation = null;
		String eucaHeader = null;
		HttpReader reader = new HttpReader("path", queue, outputFile, eucaOperation, eucaHeader);
		
		String snapshotId = "snap-12345";
		String snapshotLocation = "snapshots" + "/" + snapshotId;		
		String absoluteSnapshotPath = "/opt/eucalyptus/testreaderfile";
		String tmpStorage = "/opt/eucalyptus/";
		File file = new File(absoluteSnapshotPath);

		HttpReader snapshotGetter = new HttpReader(snapshotLocation, null, file, "GetWalrusSnapshot", "", true, tmpStorage);
		snapshotGetter.run();
		
		reader.run();
			
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Running authenticate test");
		testWalrusAuthenticationHandler();
		
		System.out.println("Running write test");
		testWriter();
		
		System.out.println("Running read test");
		testReader();

	}

}
