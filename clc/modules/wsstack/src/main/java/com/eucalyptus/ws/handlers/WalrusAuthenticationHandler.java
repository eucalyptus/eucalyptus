package com.eucalyptus.ws.handlers;

import java.io.StringReader;
import java.net.URLDecoder;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.encoders.Base64;
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
import com.eucalyptus.auth.UserCredentialProvider;
import com.eucalyptus.auth.util.AbstractKeyStore;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.MappingHttpResponse;
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
			handle(httpRequest);
		}
	}

	public void handle(MappingHttpRequest httpRequest) throws AuthenticationException
	{
		Map<String,String> parameters = httpRequest.getParameters( );
		String verb = httpRequest.getMethod().getName();
		String addr = httpRequest.getUri();

		if(httpRequest.containsHeader(StorageProperties.StorageSecurityParameters.EucaSignature.toString())) {
			//possible internal request -- perform authentication using internal credentials
			String date = httpRequest.getAndRemoveHeader(SecurityParameter.Date.toString());
			String eucaCert = httpRequest.getAndRemoveHeader(StorageProperties.StorageSecurityParameters.EucaCert.toString());
			String signature = httpRequest.getAndRemoveHeader(StorageProperties.StorageSecurityParameters.EucaSignature.toString());
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
					LOG.warn ("WalrusQuerySecurityHandler(): certificate not found in keystore");
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
			/*UserInfo admin = new UserInfo(EucalyptusProperties.NAME);
          admin.setIsAdministrator(Boolean.TRUE);
          return admin;*/
		} else if(httpRequest.containsHeader(WalrusProperties.FormField.FormUploadPolicyData.toString())) {
			String data = httpRequest.getAndRemoveHeader(WalrusProperties.FormField.FormUploadPolicyData.toString());
			String auth_part = httpRequest.getAndRemoveHeader(SecurityParameter.Authorization.toString());

			if(auth_part != null) {
				String sigString[] = getSigInfo(auth_part);
				String signature = sigString[1];				
				authenticate(httpRequest, sigString[0], signature, data);
			}
			throw new AuthenticationException("User authentication failed.");
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
			String queryKey = UserCredentialProvider.getSecretKey(accessKeyID);
			String authSig = checkSignature( queryKey, data );
			if (!authSig.equals(signature))
				throw new AuthenticationException( "User authentication failed. Could not verify signature" );
			String userName = UserCredentialProvider.getUserName( accessKeyID );
			User user = UserCredentialProvider.getUser( userName );  
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

	private String combineMultilineHeader(String header) {
		String value =  header.trim();
		String[] parts = value.split("\n");
		value = "";
		for(String part: parts) {
			part = part.trim();
			value += part + " ";
		}
		value = value.trim();

	}

	private String getCanonicalizedAmzHeaders(MappingHttpRequest httpRequest) {
		Set<String> headerNames = httpRequest.getHeaderNames();

		TreeMap amzHeaders = new TreeMap<String, String>();
		for(String headerName : headerNames) {
			String headerNameLcase = headerName.toLowerCase().trim();
			if (!headerNameLcase.startsWith("x-amz-"))
				continue;

			StringBuilder values = new StringBuilder();
			for (String headerValue: httpRequest.getHeaders(headerName)) {
				values.append(combineMultilineHeader(headerValue));
				values.append(",");
			}
			// Remove the last comma
			if (values.length() > 0)
				values.deleteCharAt(values.length() -1);

			amzHeaders.put(headerNameLcase, values.toString());
		}

		StringBuilder result = new StringBuilder();
		Iterator<String> iterator = amzHeaders.keySet().iterator();
		while(iterator.hasNext()) {
			String key = iterator.next();
			String value = (String) amzHeaders.get(key);
			result.append(key).append(":");
			result.append(value).append("\n");
		}
		return result.toString();
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
