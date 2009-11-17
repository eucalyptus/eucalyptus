package edu.ucsb.eucalyptus.cloud;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;

public class BucketLogData {
	private static final String ENTRY_FORMAT = "%s %s %s %s %s %s %s %s %s %s %s %d %d %d %d %s %s %n"; 
	private String requestId;
	private String targetBucket;
	private String targetPrefix;
	private String ownerId;
	private String bucketName;
	private String timestamp;
	private String sourceAddress;
	private String accessorId;
	private String operation;
	private String key;
	private String uri;
	private String status;
	private String error;
	private long bytesSent;
	private long objectSize;
	private long totalTime;
	private long turnAroundTime;
	private String referrer;
	private String userAgent;

	public BucketLogData() {
		key = "-";
		status = "-";
		error = "-";
		referrer = "-";
		userAgent = "-";
		status = "-";
		accessorId = "Anonymous";
		status = "200";
	}

	public BucketLogData(String requestId) {
		this();
		this.requestId = requestId;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getTargetBucket() {
		return targetBucket;
	}

	public void setTargetBucket(String targetBucket) {
		this.targetBucket = targetBucket;
	}

	public String getTargetPrefix() {
		return targetPrefix;
	}

	public void setTargetPrefix(String targetPrefix) {
		this.targetPrefix = targetPrefix;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getSourceAddress() {
		return sourceAddress;
	}

	public void setSourceAddress(String sourceAddress) {
		this.sourceAddress = sourceAddress;
	}

	public String getAccessorId() {
		return accessorId;
	}

	public void setAccessorId(String accessorId) {
		this.accessorId = accessorId;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public long getBytesSent() {
		return bytesSent;
	}

	public void setBytesSent(long bytesSent) {
		this.bytesSent = bytesSent;
	}

	public long getObjectSize() {
		return objectSize;
	}

	public void setObjectSize(long objectSize) {
		this.objectSize = objectSize;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(long totalTime) {
		this.totalTime = totalTime;
	}

	public long getTurnAroundTime() {
		return turnAroundTime;
	}

	public void setTurnAroundTime(long turnAroundTime) {
		this.turnAroundTime = turnAroundTime;
	}

	public String getReferrer() {
		return referrer;
	}

	public void setReferrer(String referrer) {
		this.referrer = referrer;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String toFormattedString () {
		return	String.format(ENTRY_FORMAT, 
				getOwnerId(),
				getBucketName(),
				"[" + getTimestamp() + "]",
				getSourceAddress(),
				getAccessorId(),
				getRequestId(),
				getOperation(),
				getKey(),
				getUri(),
				getStatus(),
				getError(),
				getBytesSent(),
				getObjectSize(),
				getTotalTime(),
				getTurnAroundTime(),
				getReferrer(),
				getUserAgent());
	}
}