package com.server;

import com.RUDP.MyServerSocket;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LibHttp implements Runnable {

    private int port;
    private String path;
    private int cntThread;
    private boolean isOverWrite;
    private boolean isVerbose;
    private boolean isBrowserCompatible;
    private int timeOut;
    private final static ReentrantLock lock=new ReentrantLock(true);
    private final static HashMap<String, ReentrantReadWriteLock> fileLockTable=new HashMap<>();

    public LibHttp(int port, String path, int cntThread, boolean isOverWrite, boolean isVerbose, int timeOut, boolean isBrowserCompatible) {
        this.port = port;
        this.path = path;
        this.cntThread=cntThread;
        this.isOverWrite=isOverWrite;
        this.isVerbose=isVerbose;
        this.isBrowserCompatible=isBrowserCompatible;
        this.timeOut=timeOut;
    }

    @Override
    public void run() {
        ExecutorService executor=null;
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("File server started, listening on port: "+port);

            executor=Executors.newFixedThreadPool(cntThread);

            MyServerSocket myServerSocket = new MyServerSocket(port);

            while (true){
                MyServerSocket newServerSocket = myServerSocket.accept();
//                Socket clientSocket=serverSocket.accept();
                Runnable worker=new Worker(this, newServerSocket,path,timeOut,isOverWrite,isBrowserCompatible,isVerbose);
                executor.execute(worker);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (executor!=null) executor.shutdown();
        }

    }

    public ReentrantReadWriteLock getLock(File file, long tId) {
        lock.lock();
        try{
            String filePath=file.getAbsolutePath();
            System.out.println("Thread " + tId + " is waiting for lock");
            if (!fileLockTable.containsKey(filePath))
                fileLockTable.put(filePath, new ReentrantReadWriteLock(true));
            return fileLockTable.get(filePath);
        } finally {
            lock.unlock();
        }
    }


    public void removeLock(File file, long tId, boolean isWriter) {
        lock.lock();
        try {
            String filePath=file.getAbsolutePath();
            if (fileLockTable.containsKey(filePath)) {
                System.out.println("Thread " + tId + " is releasing lock");
                ReentrantReadWriteLock fileLock=fileLockTable.get(filePath);
                if (fileLock.hasQueuedThreads() || fileLock.getReadLockCount()>1){
                    if (isWriter){
                        fileLock.writeLock().unlock();
                    } else {
                        fileLock.readLock().unlock();
                    }
                }
                else
                    fileLockTable.remove(filePath);
            }
        } finally {
            lock.unlock();
        }
    }

}
