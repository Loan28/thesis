package com.chainML.service;

import com.google.protobuf.ByteString;
import com.chainML.pb.*;
import io.grpc.ManagedChannel;
import com.chainML.pb.chainMLServiceGrpc.chainMLServiceBlockingStub;

import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.conscrypt.io.IoUtils;
import org.junit.Assert;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.String;


public class chainMLClient {
    private static final Logger logger = Logger.getLogger(chainServer.class.getName());

    private final ManagedChannel channel;
    private final chainMLServiceBlockingStub blockingStub;
    private final chainMLServiceGrpc.chainMLServiceStub asyncStub;

    public chainMLClient(String host, int port){
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = chainMLServiceGrpc.newBlockingStub(channel);
        asyncStub = chainMLServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    //Function to download file from the server, arg: file name
    public ByteArrayOutputStream downloadFile(String fileName) {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final AtomicBoolean completed = new AtomicBoolean(false);

        StreamObserver<DataChunk> streamObserver = new StreamObserver<DataChunk>() {
            @Override
            public void onNext(DataChunk dataChunk) {
                try {
                    baos.write(dataChunk.getData().toByteArray());
                } catch (IOException e) {
                    logger.warning("error on write to byte array stream" + e);
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("downloadFile() error" + t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("downloadFile() has been completed!");
                completed.compareAndSet(false, true);
                finishLatch.countDown();
            }
        };

        try {

            DownloadFileRequest.Builder builder = DownloadFileRequest
                    .newBuilder()
                    .setFileName(fileName);

            asyncStub.downloadFile(builder.build(), streamObserver);

            finishLatch.await(5, TimeUnit.MINUTES);

            if (!completed.get()) {
                throw new Exception("The downloadFile() method did not complete");
            }

        } catch (Exception e) {
            logger.warning("The downloadFile() method did not complete");
        }

        return baos;
    }

    //Function to upload file to the server, arg: file path
    public void uploadImage(String imagePath) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<UploadImageRequest> requestObserver = asyncStub.withDeadlineAfter(5,TimeUnit.SECONDS)
                .uploadImage(new StreamObserver<UploadImageResponse>() {
                    @Override
                    public void onNext(UploadImageResponse response) {

                        logger.info("receive response: " + response);
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.log(Level.SEVERE, "upload failed: " + t);
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("image uploaded");
                        finishLatch.countDown();
                    }
                });

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(imagePath);
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "cannot read image file " + e.getMessage());
        }

        String imageType = imagePath.substring(imagePath.lastIndexOf("."));
        ImageInfo info = ImageInfo.newBuilder().setImageType(imageType).build();
        UploadImageRequest request = UploadImageRequest.newBuilder().setInfo(info).build();

        try {
            requestObserver.onNext(request);
            logger.info("sent image info" + info);

            byte[] buffer = new byte[1024];
            while (true) {
                int n = fileInputStream.read(buffer);
                if (n <= 0) {
                    break;
                }

                if (finishLatch.getCount() == 0) {
                    return;
                }
                request = UploadImageRequest.newBuilder()
                        .setChunkData(ByteString.copyFrom(buffer, 0, n))
                        .build();
                requestObserver.onNext(request);
                logger.info("sent image chunk with size: " + n);
            }
        }catch (Exception e){
            logger.log(Level.SEVERE, "unexcepted error: " + e.getMessage());
            requestObserver.onError(e);
            return;
        }

        requestObserver.onCompleted();

        if (!finishLatch.await(1, TimeUnit.MINUTES)){
            logger.warning("request cannot finish within 1 minute");
        }
    }

    public void downloadImage(String fileName) throws IOException {
        String property = "java.io.tmpdir";
        String tmpDir = System.getProperty(property);

        ByteArrayOutputStream imageOutputStream = downloadFile(fileName);
        byte[] bytes = imageOutputStream.toByteArray();

        ImageIcon imageIcon = new ImageIcon(bytes);
        Image image = imageIcon.getImage();
        int width = 912;
        int height = 513;
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics g = bi.getGraphics();
        g.drawImage(image, 0, 0, null);
        File tmpFile = new File("tmp/" + fileName);
        ImageIO.write(bi, "png", tmpFile);
        logger.info("File has been downloaded --> " + tmpFile.getAbsolutePath());
    }

    public static void main(String[] args) throws InterruptedException {

        String Android = "192.168.1.77";
        chainMLClient client = new chainMLClient(Android, 50051);

        try {
            client.uploadImage("tmp/goose.jpg");
           // client.downloadFile("laptop.jpg");
        } finally {
            client.shutdown();
        }
    }

}
