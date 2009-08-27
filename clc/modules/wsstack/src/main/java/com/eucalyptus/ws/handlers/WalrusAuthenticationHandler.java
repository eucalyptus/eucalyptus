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
package com.eucalyptus.ws.handlers;

import java.io.StringReader;
import java.net.URLDecoder;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.groovy.JsonSlurper;

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.encoders.Base64;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.UserCredentialProvider;
import com.eucalyptus.auth.util.AbstractKeyStore;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.util.StorageProperties;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.auth.User;

@ChannelPipelineCoverage("one")
public class WalrusAuthenticationHandler extends MessageStackHandler {
	private static Logger LOG = Logger.getLogger( WalrusAuthenticationHandler.class );
	private final static long EXPIRATION_LIMIT = 900000;
	public enum SecurityParameter {
		AWSAccessKeyId,
		Timestamp,
		Expires,
		Signature,
		Authorization,
		Date,
		Content_MD5,
		Content_Type
	}

	@Override
	public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
		if ( event.getMessage( ) instanceof MappingHttpRequest ) {
			MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
			if(httpRequest.getMethod().getName().equals(WalrusProperties.HTTPVerb.POST.toString())) {
				Map<String, String> formFields = httpRequest.getFormFields();
				processPOSTParams(httpRequest, formFields);
				checkPolicy(httpRequest, formFields);
			}
			handle(httpRequest);
		}
	}

	public void handle(MappingHttpRequest httpRequest) throws AuthenticationException
	{
		Map<String,String> parameters = httpRequest.getParameters( );
		String verb = httpRequest.getMethod().getName();
		String addr = httpRequest.getUri();

		if(httpRequest.containsHeader(StorageProperties.StorageParameters.EucaSignature.toString())) {
			//possible internal request -- perform authentication using internal credentials
			String date = httpRequest.getAndRemoveHeader(SecurityParameter.Date.toString());
			String eucaCert = httpRequest.getAndRemoveHeader(StorageProperties.StorageParameters.EucaCert.toString());
			String signature = httpRequest.getAndRemoveHeader(StorageProperties.StorageParameters.EucaSignature.toString());
			String data = verb + "\n" + date + "\n" + addr + "\n";

			Signature sig;
			boolean valid = false;
			try {
				byte[] bytes = Base64.decode(eucaCert);
				String certString = new String(bytes);
				PEMReader pemReader = new PEMReader(new StringReader(certString));
				X509Certificate cert = (X509Certificate) pemReader.readObject();
				AbstractKeyStore keyStore = EucaKeyStore.getInstance();
				if (keyStore.getCertificateAlias(cert) != null) {
					//cert found in keystore
					PublicKey publicKey = cert.getPublicKey();
					sig = Signature.getInstance("SHA1withRSA");

					sig.initVerify(publicKey);
					sig.update(data.getBytes());
					valid = sig.verify(Base64.decode(signature));
				} else {
					LOG.warn ("Authentication: certificate not found in keystore");
				}
			} catch (Exception ex) {
				LOG.warn ("Authentication exception: " + ex.getMessage());
				ex.printStackTrace();
			}

			if(!valid) {
				throw new AuthenticationException( "User authentication failed." );
			}
			//TODO: set userinfo in message
			//run as admin
			try {
				User user = UserCredentialProvider.getUser( "admin" );
				user.setIsAdministrator(true);
				httpRequest.setUser( user );
			} catch (NoSuchUserException e) {
				throw new AuthenticationException( "User authentication failed." );
			}  
		} else if(httpRequest.getFormFields().size() > 0) {
			String data = httpRequest.getAndRemoveHeader(WalrusProperties.FormField.FormUploadPolicyData.toString());
			String auth_part = httpRequest.getAndRemoveHeader(SecurityParameter.Authorization.toString());

			if(auth_part != null) {
				String sigString[] = getSigInfo(auth_part);
				String signature = sigString[1];				
				authenticate(httpRequest, sigString[0], signature, data);
			} else {
				throw new AuthenticationException("User authentication failed.");
			}
		} else {
			//external user request
			String content_md5 = httpRequest.getAndRemoveHeader("Content-MD5");
			content_md5 = content_md5 == null ? "" : content_md5;
			String content_type = httpRequest.getHeader(WalrusProperties.CONTENT_TYPE);
			content_type = content_type == null ? "" : content_type;

			String targetHost = httpRequest.getHeader(HttpHeaders.Names.HOST);
			if(targetHost.contains(".walrus")) {
				String bucket = targetHost.substring(0, targetHost.indexOf(".walrus"));
				addr = "/" + bucket + addr;
			}
			String[] addrStrings = addr.split("\\?");
			String addrString = addrStrings[0];

			if(addrStrings.length > 1) {
				for(WalrusProperties.SubResource subResource : WalrusProperties.SubResource.values()) {
					if(addr.endsWith(subResource.toString().toLowerCase())) {
						addrString += "?" + subResource.toString().toLowerCase();
						break;
					}
				}
			}

			if(httpRequest.containsHeader(SecurityParameter.Authorization.toString())) {
				String date;
				String verifyDate;
				if(httpRequest.containsHeader("x-amz-date")) {
					date = "";
					verifyDate = httpRequest.getHeader("x-amz-date");
				} else {
					date =  httpRequest.getAndRemoveHeader(SecurityParameter.Date.toString());
					verifyDate = date;
					if(date == null || date.length() <= 0)
						throw new AuthenticationException("User authentication failed. Date must be specified.");
				}

				try {
					Date dateToVerify = DateUtil.parseDate(verifyDate);
					Date currentDate = new Date();
					if(Math.abs(currentDate.getTime() - dateToVerify.getTime()) > EXPIRATION_LIMIT)
						throw new AuthenticationException("Message expired. Sorry.");
				} catch(Exception ex) {
					throw new AuthenticationException("Unable to parse date.");
				}
				String data = verb + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n" +  getCanonicalizedAmzHeaders(httpRequest) + addrString;

				String auth_part = httpRequest.getAndRemoveHeader(SecurityParameter.Authorization.toString());
				String sigString[] = getSigInfo(auth_part);
				String signature = sigString[1];
				authenticate(httpRequest, sigString[0], signature, data);
			} else if(parameters.containsKey(SecurityParameter.AWSAccessKeyId.toString())) {
				//query string authentication
				String accesskeyid = parameters.remove(SecurityParameter.AWSAccessKeyId.toString());
				try {
					String signature = URLDecoder.decode(parameters.remove(SecurityParameter.Signature.toString()), "UTF-8");
					if(signature == null) {
						throw new AuthenticationException("User authentication failed. Null signature.");
					}
					String expires = parameters.remove(SecurityParameter.Expires.toString());
					if(expires == null) {
						throw new AuthenticationException("Authentication failed. Expires must be specified.");
					}
					if(checkExpires(expires)) {
						String stringToSign = verb + "\n" + content_md5 + "\n" + content_type + "\n" + Long.parseLong(expires) + "\n" + getCanonicalizedAmzHeaders(httpRequest) + addrString;
						authenticate(httpRequest, accesskeyid, signature, stringToSign);
					} else {
						throw new AuthenticationException("Cannot process request. Expired.");
					}
				} catch (Exception ex) {
					throw new AuthenticationException("Could not verify request " + ex.getMessage());
				}
			} else{
				//anonymous request              
			}
		}
	}

	private boolean checkExpires(String expires) {
		Long expireTime = Long.parseLong(expires);
		Long currentTime = new Date().getTime() / 1000;
		if(currentTime > expireTime)
			return false;
		return true;
	}

	private void authenticate(MappingHttpRequest httpRequest, String accessKeyID, String signature, String data) throws AuthenticationException {
		signature = signature.replaceAll("=", "");
		try {
	//		String queryKey = UserCredentialProvider.getSecretKey(accessKeyID);
	//		String authSig = checkSignature( queryKey, data );
			//if (!authSig.equals(signature))
		//		throw new AuthenticationException( "User authentication failed. Could not verify signature" );
	//		String userName = UserCredentialProvider.getUserName( accessKeyID );
			User user = UserCredentialProvider.getUser( "admin");// userName );  
			httpRequest.setUser( user );
		} catch(Exception ex) {
			throw new AuthenticationException( "User authentication failed. Unable to obtain query key" );
		}
	}

	private String[] getSigInfo (String auth_part) {
		int index = auth_part.lastIndexOf(" ");
		String sigString = auth_part.substring(index + 1);
		return sigString.split(":");
	}

	private String getCanonicalizedAmzHeaders(MappingHttpRequest httpRequest) {
		String result = "";
		Set<String> headerNames = httpRequest.getHeaderNames();

		TreeMap amzHeaders = new TreeMap<String, String>();
		for(String headerName : headerNames) {
			String headerNameString = headerName.toLowerCase().trim();
			if(headerNameString.startsWith("x-amz-")) {
				String value =  httpRequest.getHeader(headerName).trim();
				String[] parts = value.split("\n");
				value = "";
				for(String part: parts) {
					part = part.trim();
					value += part + " ";
				}
				value = value.trim();
				if(amzHeaders.containsKey(headerNameString)) {
					String oldValue = (String) amzHeaders.remove(headerNameString);
					oldValue += "," + value;
					amzHeaders.put(headerNameString, oldValue);
				} else {
					amzHeaders.put(headerNameString, value);
				}
			}
		}

		Iterator<String> iterator = amzHeaders.keySet().iterator();
		while(iterator.hasNext()) {
			String key = iterator.next();
			String value = (String) amzHeaders.get(key);
			result += key + ":" + value + "\n";
		}
		return result;
	}

	protected String checkSignature( final String queryKey, final String subject ) throws AuthenticationException
	{
		SecretKeySpec signingKey = new SecretKeySpec( queryKey.getBytes(), Hashes.Mac.HmacSHA1.toString() );
		try
		{
			Mac mac = Mac.getInstance( Hashes.Mac.HmacSHA1.toString() );
			mac.init( signingKey );
			byte[] rawHmac = mac.doFinal( subject.getBytes() );
			return new String(Base64.encode( rawHmac )).replaceAll( "=", "" );
		}
		catch ( Exception e )
		{
			LOG.error( e, e );
			throw new AuthenticationException( "Failed to compute signature" );
		}
	}

	private void processPOSTParams(MappingHttpRequest httpRequest, Map<String, String> formFields) throws AuthenticationException {
		String contentType = httpRequest.getHeader(WalrusProperties.CONTENT_TYPE);
		if(contentType != null) {
			if(contentType.startsWith(WalrusProperties.MULTIFORM_DATA_TYPE)) {
				String boundary = getFormFieldKeyName(contentType, "boundary");
				boundary = "--" + boundary + "\r\n";
				String message = getMessageString(httpRequest);
				String[] parts = message.split(boundary);
				for(String part : parts) {
					Map<String, String> keyMap = getFormField(part, "name");
					Set<String> keys = keyMap.keySet();
					for(String key : keys) {
						if(WalrusProperties.FormField.file.toString().equals(key)) {
							getFirstChunk(formFields, part, boundary);
						}
						formFields.put(key, keyMap.get(key));
					}
				}
			}
			String[] target = getTarget(httpRequest);
			formFields.put(WalrusProperties.FormField.bucket.toString(), target[0]);
		} else {
			throw new AuthenticationException("No Content-Type specified");
		}
	}

	private void checkPolicy(MappingHttpRequest httpRequest, Map<String, String> formFields) throws AuthenticationException {
		if(formFields.containsKey(WalrusProperties.FormField.policy.toString())) {
			String authenticationHeader = "";
			String policy = new String(Base64.decode(formFields.remove(WalrusProperties.FormField.policy.toString())));
			String policyData;
			try {
				policyData = new String(Base64.encode(policy.getBytes()));
			} catch (Exception ex) {
				LOG.warn(ex, ex);
				throw new AuthenticationException("error reading policy data.");
			}
			//parse policy
			try {
				JsonSlurper jsonSlurper = new JsonSlurper();
				JSONObject policyObject = (JSONObject)jsonSlurper.parseText(policy);
				String expiration = (String) policyObject.get(WalrusProperties.PolicyHeaders.expiration.toString());
				if(expiration != null) {
					Date expirationDate = DateUtils.parseIso8601DateTimeOrDate(expiration);
					if((new Date()).getTime() > expirationDate.getTime()) {
						LOG.warn("Policy has expired.");
						//TODO: currently this will be reported as an invalid operation
						//Fix this to report a security exception
						throw new AuthenticationException("Policy has expired.");
					}
				}
				List<String> policyItemNames = new ArrayList<String>();

				JSONArray conditions = (JSONArray) policyObject.get(WalrusProperties.PolicyHeaders.conditions.toString());
				for (int i = 0 ; i < conditions.size() ; ++i) {
					Object policyItem = conditions.get(i);
					if(policyItem instanceof JSONObject) {
						JSONObject jsonObject = (JSONObject) policyItem;
						if(!exactMatch(jsonObject, formFields, policyItemNames)) {
							LOG.warn("Policy verification failed. ");
							throw new AuthenticationException("Policy verification failed.");
						}
					} else if(policyItem instanceof  JSONArray) {
						JSONArray jsonArray = (JSONArray) policyItem;
						if(!partialMatch(jsonArray, formFields, policyItemNames)) {
							LOG.warn("Policy verification failed. ");
							throw new AuthenticationException("Policy verification failed.");
						}
					}
				}

				Set<String> formFieldsKeys = formFields.keySet();
				for(String formKey : formFieldsKeys) {
					if(formKey.startsWith(WalrusProperties.IGNORE_PREFIX))
						continue;
					boolean fieldOkay = false;
					for(WalrusProperties.IgnoredFields field : WalrusProperties.IgnoredFields.values()) {
						if(formKey.equals(field.toString())) {
							fieldOkay = true;
							break;
						}
					}
					if(fieldOkay)
						continue;
					if(policyItemNames.contains(formKey))
						continue;
					LOG.warn("All fields except those marked with x-ignore- should be in policy.");
					throw new AuthenticationException("All fields except those marked with x-ignore- should be in policy.");
				}
			} catch(Exception ex) {
				//rethrow
				LOG.warn(ex);
				if(ex instanceof AuthenticationException)
					throw (AuthenticationException)ex;
			}
			//all form uploads without a policy are anonymous
			if(formFields.containsKey(WalrusProperties.FormField.AWSAccessKeyId.toString())) {
				String accessKeyId = formFields.remove(WalrusProperties.FormField.AWSAccessKeyId.toString());
				authenticationHeader += "AWS" + " " + accessKeyId + ":";
			}
			if(formFields.containsKey(WalrusProperties.FormField.signature.toString())) {
				String signature = formFields.remove(WalrusProperties.FormField.signature.toString());
				authenticationHeader += signature;
				httpRequest.addHeader(WalrusAuthenticationHandler.SecurityParameter.Authorization.toString(), authenticationHeader);
			}
			httpRequest.addHeader(WalrusProperties.FormField.FormUploadPolicyData.toString(), policyData);
		}
	}

	private Map<String, String> getFormField(String message, String key) {
		Map<String, String> keymap = new HashMap<String, String>();
		String[] parts = message.split(";");
		if(parts.length >= 2) {
			if (parts[1].contains(key + "=")) {
				String keystring = parts[1].substring(parts[1].indexOf('=') + 1);
				if(parts.length == 2) {
					String[] keyparts = keystring.split("\r\n\r\n");
					String keyName = keyparts[0];
					keyName = keyName.replaceAll("\"", "");
					String value = keyparts[1].replaceAll("\r\n", "");
					keymap.put(keyName, value);
				} else {
					String keyName = keystring.trim();
					keyName = keyName.replaceAll("\"", "");
					String valuestring = parts[2].substring(parts[2].indexOf('=') + 1, parts[2].indexOf("\r\n")).trim();
					String value = valuestring.replaceAll("\"", "");
					keymap.put(keyName, value);
				}
			}
		}
		return keymap;		
	}

	private String getFormFieldKeyName(String message, String key) {
		String[] parts = message.split(";");
		if(parts.length > 1) {
			if (parts[1].contains(key + "=")) {
				String keystring = parts[1].substring(parts[1].indexOf('=') + 1);
				String[] keyparts = keystring.split("\r\n\r\n");
				String keyName = keyparts[0];
				keyName = keyName.replaceAll("\r\n", "");
				keyName = keyName.replaceAll("\"", "");
				return keyName;
			}
		}
		return null;		
	}

	private String getMessageString(MappingHttpRequest httpRequest) {
		ChannelBuffer buffer = httpRequest.getContent( );
		buffer.markReaderIndex( );
		byte[] read = new byte[buffer.readableBytes( )];
		buffer.readBytes( read );
		return new String( read );
	}

	private void getFirstChunk(Map<String, String>formFields, String part, String boundary) {
		int endValue = part.indexOf("\r\n\r\n");
		int startValue = part.indexOf(WalrusProperties.CONTENT_TYPE + ":") + WalrusProperties.CONTENT_TYPE.length() + 1;
		if(endValue > startValue) {
			String contentType = part.substring(startValue, endValue);
			formFields.put(WalrusProperties.CONTENT_TYPE, contentType);			
			String firstChunk = part.substring(endValue + "\r\n\r\n".length(), part.length() - "\r\n".length());
			formFields.put(WalrusProperties.IGNORE_PREFIX + "FirstDataChunk", firstChunk);
		}
	}

	private boolean exactMatch(JSONObject jsonObject, Map formFields, List<String> policyItemNames) {
		Iterator<String> iterator = jsonObject.keys();
		boolean returnValue = false;
		while(iterator.hasNext()) {
			String key = iterator.next();
			key = key.replaceAll("\\$", "");
			policyItemNames.add(key);
			try {
				if(jsonObject.get(key).equals(formFields.get(key)))
					returnValue = true;
				else
					returnValue = false;
			} catch(Exception ex) {
				ex.printStackTrace();
				return false;
			}
		}
		return returnValue;
	}

	private boolean partialMatch(JSONArray jsonArray, Map<String, String> formFields, List<String> policyItemNames) {
		boolean returnValue = false;
		if(jsonArray.size() != 3)
			return false;
		try {
			String condition = (String) jsonArray.get(0);
			String key = (String) jsonArray.get(1);
			key = key.replaceAll("\\$", "");
			policyItemNames.add(key);
			String value = (String) jsonArray.get(2);
			if(condition.contains("eq")) {
				if(value.equals(formFields.get(key)))
					returnValue = true;
			} else if(condition.contains("starts-with")) {
				if(!formFields.containsKey(key))
					return false;
				if(formFields.get(key).startsWith(value))
					returnValue = true;
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return returnValue;
	}

	private static String[] getTarget(MappingHttpRequest httpRequest) {
		String operationPath = httpRequest.getServicePath().replaceAll(WalrusProperties.walrusServicePath, "");
		operationPath = operationPath.replaceAll("/{2,}", "/");
		if(operationPath.startsWith("/"))
			operationPath = operationPath.substring(1);
		return operationPath.split("/");
	}

	@Override
	public void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent exceptionEvent ) throws Exception {
		LOG.info("[exception " + exceptionEvent + "]");
		final HttpResponse response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR );
		DownstreamMessageEvent newEvent = new DownstreamMessageEvent( ctx.getChannel( ), ctx.getChannel().getCloseFuture(), response, null );
		ctx.sendDownstream( newEvent );
		newEvent.getFuture( ).addListener( ChannelFutureListener.CLOSE );
	}

	@Override
	public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
	}
}
