package edu.ucsb.eucalyptus.admin.server.extensions.store;

import java.util.Map;
import java.util.List;

import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpException;

import edu.ucsb.eucalyptus.admin.server.EucalyptusWebBackendImpl;
import edu.ucsb.eucalyptus.admin.client.UserInfoWeb;
import edu.ucsb.eucalyptus.admin.client.extensions.store.ImageStoreService;

public class ImageStoreServiceImpl extends RemoteServiceServlet
        implements ImageStoreService {

    /* These are fixed: */
    private final static String API_VERSION = "2009-10-01";
    private final static String SIGNATURE_VERSION = "2";
    private final static String SIGNATURE_METHOD = "HmacSHA256";
    private final static int EXPIRES_DELAY_SECONDS = 30;


    public String requestJSON(String sessionId, Method method, String uri,
                              Parameter[] params) {

        UserInfoWeb user;
        try {
            user = EucalyptusWebBackendImpl.getUserRecord (sessionId);
        } catch (Exception e) {
            return errorJSON("Session authentication error: " + e.getMessage());
        }

        NameValuePair[] finalParams;
        try {
            finalParams = getFinalParameters(method, uri, params, user);
        } catch (MalformedURLException e) {
            return errorJSON("Malformed URL: " + uri);
        }

        HttpClient client = new HttpClient();

        HttpMethod httpMethod;
        if (method == Method.GET) {
            GetMethod getMethod = new GetMethod(uri);
            httpMethod = getMethod;
            getMethod.setQueryString(finalParams);
        } else if (method == Method.POST) {
            PostMethod postMethod = new PostMethod(uri);
            httpMethod = postMethod;
            postMethod.addParameters(finalParams);
        } else {
            throw new UnsupportedOperationException("Unknown method");
        }

        try {
            int statusCode = client.executeMethod(httpMethod);
            return httpMethod.getResponseBodyAsString();
        } catch (HttpException e) {
            return errorJSON("Protocol error: " + e.getMessage());
        } catch (IOException e) {
            return errorJSON("Proxy error: " + e.getMessage());
        } finally {
            httpMethod.releaseConnection();
        }
    }

    private String errorJSON(String errorMessage) {
        return "{\"error-message\": \"" + escapeString(errorMessage) + "\"}";
    }

    private String escapeString(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private NameValuePair[] getFinalParameters(Method method, String uri,
                                               Parameter[] params, UserInfoWeb user)
            throws MalformedURLException {

        if (params == null) {
            // Simplify the logic below.
            params = new Parameter[0];
        }

        URL url = new URL(uri);

        SignatureGenerator signatureGenerator =
            new SignatureGenerator(method.toString(), url.getHost(),
                                   url.getPort(), url.getPath());

        Parameter[] finalParams = new Parameter[params.length + 7];
        System.arraycopy(params, 0, finalParams, 0, params.length);

        int i = params.length;
        finalParams[i++] = new Parameter("SignatureMethod", SIGNATURE_METHOD);
        finalParams[i++] = new Parameter("SignatureVersion", SIGNATURE_VERSION);
        finalParams[i++] = new Parameter("Version", API_VERSION);
        finalParams[i++] = new Parameter("ClientId", user.getQueryId());
        finalParams[i++] = new Parameter("Expires",
                                         new Long(
                                            (System.currentTimeMillis() / 1000)
                                            + EXPIRES_DELAY_SECONDS
                                         ).toString());
        finalParams[i++] = new Parameter("Nonce",
                                         new Long(System.nanoTime()).toString());

        for (Parameter param : finalParams) {
            if (param != null) {
                signatureGenerator.addParameter(param.getName(),
                                                param.getValue());
            }
        }

        String signature = signatureGenerator.getSignature(user.getSecretKey());
        finalParams[i++] = new Parameter("Signature", signature);

        return getNameValuePairs(finalParams);
    }

    private NameValuePair[] getNameValuePairs(Parameter[] params) {
        NameValuePair[] pairs = new NameValuePair[params.length];
        for (int i = 0; i != params.length; i++) {
            pairs[i] = new NameValuePair(params[i].getName(),
                                         params[i].getValue());
        }
        return pairs;
    }

}
