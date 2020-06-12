package com.chainvideoandroid;

import android.content.res.AssetManager;

import com.chainML.pb.ImageInfo;
import com.chainML.pb.UploadImageRequest;
import com.chainML.pb.UploadImageResponse;
import com.chainML.pb.chainMLServiceGrpc;
import com.chainML.pb.chainMLServiceGrpc.chainMLServiceBlockingStub;
import com.google.protobuf.ByteString;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class chainMLClient {
    private static final Logger logger = Logger.getLogger(chainMLClient.class.getName());

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


    //Function to upload file to the server, arg: file path
    public void uploadImage(String imagePath, AssetManager am) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<UploadImageRequest> requestObserver = asyncStub.withDeadlineAfter(5, TimeUnit.SECONDS)
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


        InputStream fileInputStream = null;
        try {
            fileInputStream = am.open("goose.jpg");
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "cannot read image file " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
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
}
