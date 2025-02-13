package com.example;

import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3;

import java.util.List;

import com.emc.ecs.nfsclient.nfs.io.Nfs3File;

public class NfsTest {
    public static void main(String[] args) {
        try {
            //  NFS server connection parameters
            String server = "192.168.153.154";
            String exportPath = "/nfs_share";  // Try root path first   
            
            // Create NFS client
            Nfs3 nfs3Client = new Nfs3(server+":"+ exportPath, 0,0, 3); 
            
            // Create NFS file object
            Nfs3File nfsFile = new Nfs3File(nfs3Client, "/");
            
            // Test connection by listing files
            System.out.println("Connected to NFS server: " + server);
            System.out.println("Listing files in root directory:");
            List<String> files = nfsFile.list();
            if (files != null) {
                for (String file : files) {
                    System.out.println(file);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error connecting to NFS server:");
            e.printStackTrace();
        }
    }
}
