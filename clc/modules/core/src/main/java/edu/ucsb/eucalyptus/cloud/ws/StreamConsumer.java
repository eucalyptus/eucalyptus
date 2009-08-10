package edu.ucsb.eucalyptus.cloud.ws;

import com.eucalyptus.util.WalrusProperties;

import java.io.*;

public class StreamConsumer extends Thread {
    private InputStream is;
    private File file;
    private String returnValue;

    public StreamConsumer(InputStream is) {
        this.is = is;
        returnValue = "";
    }

    public StreamConsumer(InputStream is, File file) {
        this(is);
        this.file = file;
    }

    public String getReturnValue() {
        return returnValue;
    }

    public void run() {
        try {
            BufferedInputStream inStream = new BufferedInputStream(is);
            BufferedOutputStream outStream = null;
            if (file != null) {
                outStream = new BufferedOutputStream(new FileOutputStream(file));
            }
            byte[] bytes = new byte[WalrusProperties.IO_CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = inStream.read(bytes)) > 0) {
                returnValue += new String(bytes, 0, bytesRead);
                if (outStream != null) {
                    outStream.write(bytes, 0, bytesRead);
                }
            }
            if (outStream != null)
                outStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
