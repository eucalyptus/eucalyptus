
package com.eucalyptus.util;
import com.eucalyptus.vm.BundleInstanceType;
import org.bouncycastle.util.encoders.Base64;
import org.apache.log4j.Logger;
public class BundleInstanceChecker {	
        private static Logger LOG = Logger.getLogger( BundleInstanceChecker.class );
	public static void check(final BundleInstanceType request) throws EucalyptusCloudException
	{
		String policy = new String(Base64.decode(request.getUploadPolicy()));
		String bucketName = request.getBucket();
		String prefix = request.getPrefix();
		
		if(policy==null || bucketName==null || prefix==null)
			throw new EucalyptusCloudException("One or more required parameters are null");
	        
		LOG.info("bundle-instance policy: "+policy);
		// check if the policy is not user-generated one
		// "expiration": "2011-07-01T16:52:13","conditions": [{"bucket": "windowsbundle" },{"acl": "ec2-bundle-read" },["starts-with", "$key", "prefix"
		int idxOpenBracket = policy.indexOf("{");
		int idxClosingBracket = policy.lastIndexOf("}");
		if(idxOpenBracket<0 || idxClosingBracket <0 || idxOpenBracket >= idxClosingBracket)
			throw new EucalyptusCloudException("Custom policy is not acceptable for bundle instance");
		
		String bucketAndAcl = policy.substring(idxOpenBracket, idxClosingBracket-idxOpenBracket);
		if(!bucketAndAcl.contains(bucketName))
			throw new EucalyptusCloudException("Custom policy is not acceptable for bundle instance");
		if(!bucketAndAcl.contains("ec2-bundle-read"))
			throw new EucalyptusCloudException("Custom policy is not acceptable for bundle instance");
		 
		// check if the bucket name follows the ec2 convention
		if(!checkBucketName(bucketName))
			throw new EucalyptusCloudException("Bucket name is invalid");		
		
		// check if the prefix name starts with "windows"
		if(!prefix.startsWith("windows"))
			throw new EucalyptusCloudException("Prefix name should start with 'windows'");		
	}
	
	/// this is a copy of edu.ucsb.eucalyptus.cloud.ws.WalrusManager.checkBucketName
	/// could not reference it because of the build order problem
	private static boolean checkBucketName(String bucketName) {
                if(!bucketName.matches("^[a-z0-9][a-z0-9._-]+"))
                        return false;
		if(bucketName.length() < 3 || bucketName.length() > 255)
			return false;
		String[] addrParts = bucketName.split("\\.");
		boolean ipFormat = true;
		if(addrParts.length == 4) {
			for(String addrPart : addrParts) {
				try {
					Integer.parseInt(addrPart);
				} catch(NumberFormatException ex) {
					ipFormat = false;
					break;
				}
			}
		} else {
			ipFormat = false;
		}		
		if(ipFormat)
			return false;
		return true;
	}

}
