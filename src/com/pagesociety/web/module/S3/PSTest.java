package com.pagesociety.web.module.S3;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;


import com.pagesociety.web.module.S3.amazon.AWSAuthConnection;
import com.pagesociety.web.module.S3.amazon.ListAllMyBucketsResponse;
import com.pagesociety.web.module.S3.amazon.Response;



public class PSTest 
{
    static final String awsAccessKeyId = "0EWEARYY0QDGGN3J8602";
    static final String awsSecretAccessKey = "afodoPQQ7K8NmkM54JDhSGGKKHjPhwwYT6u93Uwg";
    static final String FILENAME = "C:/eclipse_workspace/S3Test/data/Rome.jpg";
    static final String BUCKET = "sassy";
    public static void main(String[] args)
    {
    	File f = new File(FILENAME);
    	PSAWSAuthConnection conn = new PSAWSAuthConnection(awsAccessKeyId, awsSecretAccessKey);
    	
        System.out.println("----- listing all my buckets -----");
        try {
        	ListAllMyBucketsResponse my_buckets = conn.listAllMyBuckets(null);
			System.out.println(my_buckets.entries);

   
    
			
			boolean bucket_exists = conn.checkBucketExists(BUCKET);
			System.out.println("does "+BUCKET+" exist? "+bucket_exists);
			if(bucket_exists)
			{
				System.out.println("BUCKET "+BUCKET+"already exists.");
				//System.exit(0);	
			}
			else
			{
				System.out.println("creating bucket "+BUCKET);
				Response r = conn.createBucket(BUCKET,AWSAuthConnection.LOCATION_DEFAULT, null);
				//TODO: need to see how we check errors//
				System.out.println("created "+BUCKET+" "+r.connection.getResponseMessage());
			}
			

			//System.out.println(conn.createBucket("postera_toph", AWSAuthConnection.LOCATION_DEFAULT, null).connection.getResponseMessage());
			//String random_name = RandomGUID.getGUID();
			//System.out.println("RGUID is "+random_name);
			
			//System.out.println("putting "+f.getAbsolutePath()+" as "+random_name);
			//FileInputStream fis = new FileInputStream(f);
			//byte[] b = getBytesFromFile(f);
			//PosteraS3Object data = new PosteraS3Object(new FileInputStream(f),f.length(),"image/jpg",PosteraAWSAuthConnection.PERMISSIONS_PUBLIC_READ,null);

        	//System.out.println(conn.put(BUCKET, random_name, data, headers).connection.getResponseMessage());
			//System.out.println(conn.streamingPut(BUCKET, random_name, data).connection.getResponseMessage());
	        
		    
			System.out.println("Getting");

		    if(conn.get(BUCKET, "blooby", null).object == null)
		    	System.out.println("NOT FOUND blooby");

		     
        } catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
			//conn.put("postera_toph", key, object, headers);
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
    
        // Get the size of the file
        long length = file.length();
    
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
    
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];
    
        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }
    
        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
    
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }


}
