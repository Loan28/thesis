package com.chainvideoandroid;


import com.chainML.pb.*;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;

import java.io.*;
import java.util.logging.Logger;

public class chainMLService extends chainMLServiceGrpc.chainMLServiceImplBase {

    private static final Logger logger = Logger.getLogger(chainMLService.class.getName());
    private ImageStore imageStore;
    private String imageID = "";
    private Context context;
    public chainMLService(ImageStore imageStore) {
        this.imageStore = imageStore;
    }

    public String get_image_id(){
        return imageID;
    }

    @Override
    public StreamObserver<UploadImageRequest> uploadImage(final StreamObserver<UploadImageResponse> responseObserver) {
        return new StreamObserver<UploadImageRequest>() {
            private String imageType;
            private ByteArrayOutputStream imageData;


            @Override
            public void onNext(UploadImageRequest request) {
                if(request.getDataCase() == UploadImageRequest.DataCase.INFO) {
                    ImageInfo info = request.getInfo();
                    logger.info("receive image info" + info);
                    imageType = info.getImageType();
                    imageData = new ByteArrayOutputStream();

                    return;

                }
                ByteString chunkData = request.getChunkData();
                logger.info("receive image chunk with size: " + chunkData.size());
                if (imageData == null) {
                    logger.info("image info was not sent before");
                    responseObserver.onError(
                            Status.INVALID_ARGUMENT
                                    .withDescription("image info was not sent before")
                                    .asRuntimeException()
                    );
                    return;
                }
                try {
                    chunkData.writeTo(imageData);
                } catch (IOException e) {
                    responseObserver.onError(
                            Status.INTERNAL
                                    .withDescription("cannot write chunk data: " + e.getMessage())
                                    .asRuntimeException()
                    );
                    return;
                }

            }

            @Override
            public void onError(Throwable t) {
                logger.warning(t.getMessage());
            }

            @Override
            public void onCompleted() {
                int imageSize = imageData.size();


                    //imageID = imageStore.Save(imageType, imageData);
                    //logger.info("receive image " + imageID);
                    MainActivity.getInstance().recognize_image3(imageData);

               /* } catch (IOException e) {
                    responseObserver.onError(
                            Status.INTERNAL
                                    .withDescription("cannot save the image to the store: " + e.getMessage())
                                    .asRuntimeException()
                    );
                }*/

                UploadImageResponse response = UploadImageResponse.newBuilder()
                        .setId(imageID)
                        .setSize(imageSize)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }

        };
    }
}
