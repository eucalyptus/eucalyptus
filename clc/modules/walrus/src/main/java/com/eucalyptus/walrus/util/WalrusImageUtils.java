package com.eucalyptus.walrus.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;

import org.apache.log4j.Logger;

import com.eucalyptus.walrus.exceptions.WalrusException;
import com.eucalyptus.walrus.util.WalrusProperties;
import com.eucalyptus.records.Logs;

public class WalrusImageUtils {
	private static final Logger LOG = Logger.getLogger(WalrusImageUtils.class);
	
	public static void decryptImage(final String encryptedImageName, final String decryptedImageName, final Cipher cipher) throws Exception {
		LOG.debug("Decrypting image file: " + decryptedImageName);
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(decryptedImageName)));
		File inFile = new File(encryptedImageName);
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(inFile));

		int bytesRead = 0;
		byte[] bytes = new byte[8192];

		try {
			while((bytesRead = in.read(bytes)) > 0) {
				byte[] outBytes = cipher.update(bytes, 0, bytesRead);
				out.write(outBytes);
			}

			byte[] outBytes = cipher.doFinal();
			out.write(outBytes);
			LOG.debug("Done decrypting: " + encryptedImageName + " into " + decryptedImageName + " successfully", null);
		} catch (IOException ex) {
			LOG.error("Failed decrypting image file " + encryptedImageName, ex );
			Logs.extreme( ).error( ex, ex );		
			throw ex;
		} finally {
			try {
				out.close();
			} catch (IOException ex) {
				LOG.error( ex );
			}
			try {
				in.close();
			} catch (IOException ex) {
				LOG.error( ex );
			}
		}
		
		
	}

	public static void assembleParts(final String name, List<String> parts) {
		LOG.debug("Assembling parts for " + name);
		FileOutputStream fileOutputStream = null;
		FileInputStream fileInputStream = null;
		try {
			fileOutputStream = new FileOutputStream(new File(name));
			FileChannel out = fileOutputStream.getChannel();
			for (String partName: parts) {
				fileInputStream = new FileInputStream(new File(partName));
				FileChannel in = fileInputStream.getChannel();
				in.transferTo(0, in.size(), out);
				in.close();
				fileInputStream.close();
			}
			out.close();
			fileOutputStream.close();
			LOG.debug("Part assembly for " + name + " completed successfully");
		} catch (Exception ex) {			
			LOG.debug("Part assembly for " + name + " failed", ex);
		} finally {
			if(fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					LOG.error(e);
				}
			}
			if(fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					LOG.error(e);
				}
			}
		}
	}
	
	public static void unzipImage(String decryptedImageName, String tarredImageName) throws Exception {
		GZIPInputStream in = new GZIPInputStream(new FileInputStream(new File(decryptedImageName)));
		File outFile = new File(tarredImageName);
		ReadableByteChannel inChannel = Channels.newChannel(in);
		FileOutputStream fileOutputStream = new FileOutputStream(outFile);
		WritableByteChannel outChannel = fileOutputStream.getChannel();

		ByteBuffer buffer = ByteBuffer.allocate(102400);
		try {
			while (inChannel.read(buffer) != -1) {
				buffer.flip();
				outChannel.write(buffer);
				buffer.clear();
			}
		} catch(IOException ex) {
			throw ex;
		} finally {
			outChannel.close();
			fileOutputStream.close();
			inChannel.close();
			in.close();
		}		
	}

	public static long untarImage(String tarredImageName, String imageName) throws Exception {
		/*TarInputStream in = new TarInputStream(new FileInputStream(new File(tarredImageName)));
       File outFile = new File(imageName);
       BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));

       TarEntry tEntry = in.getNextEntry();
       assert(!tEntry.isDirectory());

       in.copyEntryContents(out);
       out.close();
       in.close();
       return outFile.length();*/

		//Workaround because TarInputStream is broken
		Tar tarrer = new Tar();
		tarrer.untar(tarredImageName, imageName);
		File outFile = new File(imageName);
		if(outFile.exists())
			return outFile.length();
		else
			throw new WalrusException("Could not untar image " + imageName);
	}
	
	private static class StreamConsumer extends Thread
	{
		private InputStream is;
		private File file;

		public StreamConsumer(InputStream is) {
			this.is = is;
		}

		public StreamConsumer(InputStream is, File file) {
			this(is);
			this.file = file;
		}

		public void run()
		{
			BufferedOutputStream outStream = null;
			try
			{
				BufferedInputStream inStream = new BufferedInputStream(is);
				if(file != null) {
					outStream = new BufferedOutputStream(new FileOutputStream(file));
				}
				byte[] bytes = new byte[WalrusProperties.IO_CHUNK_SIZE];
				int bytesRead;
				while((bytesRead = inStream.read(bytes)) > 0) {
					if(outStream != null) {
						outStream.write(bytes, 0, bytesRead);
					}
				}
				if(outStream != null)
					outStream.close();
			} catch (IOException ex)
			{
				if(outStream != null)
					try {
						outStream.close();
					} catch (IOException e) {
						LOG.error(e);
					}
					LOG.error(ex);
			}
		}
	}

	private static class Tar {
		public void untar(String tarFile, String outFile) {
			try
			{
				Runtime rt = Runtime.getRuntime();
				Process proc = rt.exec(new String[]{ "/bin/tar", "xfO", tarFile});
				StreamConsumer error = new StreamConsumer(proc.getErrorStream());
				StreamConsumer output = new StreamConsumer(proc.getInputStream(), new File(outFile));
				error.start();
				output.start();
				int exitVal = proc.waitFor();
				output.join();
			} catch (Exception t) {
				LOG.error(t);
			}
		}
	}

}
