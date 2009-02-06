package edu.ucsb.eucalyptus.transport.http;

import edu.ucsb.eucalyptus.transport.query.WalrusQueryDispatcher;
import edu.ucsb.eucalyptus.util.WalrusDataMessage;
import edu.ucsb.eucalyptus.util.WalrusDataMessenger;
import edu.ucsb.eucalyptus.util.WalrusProperties;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.http.util.URLTemplatingUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;

public class WalrusDataFormatter implements MessageFormatter {


    public byte[] getBytes(MessageContext messageContext, OMOutputFormat format) throws AxisFault {
        //not used
        return null;
    }

    public void writeTo(MessageContext messageContext, OMOutputFormat format,
                        OutputStream outputStream, boolean preserve) throws AxisFault {

        Integer status = (Integer) messageContext.getProperty(Axis2HttpWorker.HTTP_STATUS);
        if(status == null) {
            Boolean getType = (Boolean) messageContext.getProperty(WalrusProperties.STREAMING_HTTP_GET);
            if(getType != null && getType.equals(Boolean.FALSE)) {
                try {
                    outputStream.flush();
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }   else {
                String key = (String) messageContext.getProperty("GET_KEY");
                String randomKey = (String) messageContext.getProperty("GET_RANDOM_KEY");
                Boolean isCompressed = (Boolean) messageContext.getProperty("GET_COMPRESSED");
                if(isCompressed == null)
                    isCompressed = false;

                GZIPOutputStream gzipOutStream = null;
                if(isCompressed) {
                    try {
                        gzipOutStream = new GZIPOutputStream(outputStream);
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                }

                WalrusDataMessenger messenger = WalrusQueryDispatcher.getReadMessenger();
                LinkedBlockingQueue<WalrusDataMessage> getQueue = messenger.getQueue(key, randomKey);

                WalrusDataMessage dataMessage;
                try {
                    while ((dataMessage = getQueue.take())!=null) {
                        if(WalrusDataMessage.isStart(dataMessage)) {
                            //TODO: should read size and verify
                        } else if(WalrusDataMessage.isData(dataMessage)) {
                            byte[] data = dataMessage.getPayload();
                            if(isCompressed) {
                                try {
                                    gzipOutStream.write(data);
                                } catch(Exception ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                for (byte b: data) {
                                    try {
                                        outputStream.write(b);
                                    } catch  (IOException e) {
                                        e.printStackTrace();
                                        throw new AxisFault("An error occured while writing the request");
                                    }
                                }
                            }
                        } else if(WalrusDataMessage.isEOF(dataMessage)) {
                            try {
                                if(isCompressed) {
                                    gzipOutStream.finish();
                                    gzipOutStream.flush();
                                } else {
                                    outputStream.flush();
                                }
                                messenger.removeQueue(key, randomKey);
                                break;
                            } catch  (IOException e) {
                                e.printStackTrace();
                                throw new AxisFault("An error occured while writing the request");
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public String getContentType(MessageContext messageContext, OMOutputFormat format,
                                 String soapAction) {

        String contentType = "text/plain";
        return contentType;
    }


    public URL getTargetAddress(MessageContext messageContext, OMOutputFormat format, URL targetURL)
            throws AxisFault {
        // Check whether there is a template in the URL, if so we have to replace then with data
        // values and create a new target URL.
        targetURL = URLTemplatingUtil.getTemplatedURL(targetURL, messageContext, false);

        return targetURL;
    }


    public String formatSOAPAction(MessageContext messageContext, OMOutputFormat format,
                                   String soapAction) {
        return soapAction;
    }

}
