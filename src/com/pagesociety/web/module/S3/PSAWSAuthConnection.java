package com.pagesociety.web.module.S3;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.pagesociety.web.module.S3.amazon.AWSAuthConnection;
import com.pagesociety.web.module.S3.amazon.Response;
import com.pagesociety.web.module.S3.amazon.Utils;


public class PSAWSAuthConnection extends AWSAuthConnection
{
	public static final String PERMISSIONS_PUBLIC_READ = "public-read";

	 public PSAWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey)
	 {
	       super(awsAccessKeyId, awsSecretAccessKey, true);
	 }

	    /**
	     * Check if the specified bucket exists (via a HEAD request)
	     * @param bucket The name of the bucket to check
	     * @return true if HEAD access returned success
	     */
	    public boolean checkKeyExists(String bucket,String key) throws MalformedURLException, IOException
	    {
	        HttpURLConnection response  = makeRequest("HEAD", bucket, Utils.urlencode(key), null, null);
	        int httpCode = response.getResponseCode();
	        return httpCode >= 200 && httpCode < 300;
	    }

    /**
     * Writes an object to S3.
     * @param bucket The name of the bucket to which the object will be added.
     * @param key The name of the key to use.
     * @param object An S3Object containing the data to write.
     * @param headers A Map of String to List of Strings representing the http
     * headers to pass (can be null).
     */
    public Response streamingPut(String bucket, String key, PSS3Object object)
        throws MalformedURLException, IOException
    {
    	
    	Map headers = new LinkedHashMap();
        headers.put("x-amz-acl", Arrays.asList(new String[]    { object.getPermissions()}));
        headers.put("Content-Type", Arrays.asList(new String[] { object.getContentType() }));
    	HttpURLConnection request =
            makeRequest("PUT", bucket, Utils.urlencode(key), null, headers, null);

        request.setDoOutput(true);

        long tot;
        InputStream is  = object.getInputStream();
        OutputStream os = request.getOutputStream();
        try {

          tot = 0;//7542629859369943040L; 
          System.out.println("tot is "+tot);
          System.out.println("long max is "+Long.MAX_VALUE);
          byte[] buffer = new byte[16384];
          int l;
          while ((l = is.read(buffer)) != -1) {
            os.write(buffer, 0, l);
            os.flush();
            tot += l;
            double pct = (tot/(double)object.getSize())*100.0;
            System.out.println("WROTE "+pct+"%");
          }
        } finally {
         is.close();
         os.close();
        }
        Response r = new Response(request);
        int code = r.connection.getResponseCode();
        System.out.println("STREAMING PUT RESPONSE CODE "+code+" "+r.connection.getResponseMessage());
        return r;
    }

}
