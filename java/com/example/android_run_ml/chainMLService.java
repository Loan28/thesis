package com.chainML.service;


import com.chainML.pb.*;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.util.logging.Logger;

public class chainMLService extends chainMLServiceGrpc.chainMLServiceImplBase {

    private static final Logger logger = Logger.getLogger(chainMLService.class.getName());
    private ImageStore imageStore;

    public chainMLService(ImageStore imageStore) {

        this.imageStore = imageStore;
    }

    @Override
    public void downloadFile(DownloadFileRequest request, StreamObserver<DataChunk> responseObserver) {
        try {
            String fileName = "/files/laptop.jpg";

            // read the file and convert to a byte array
            InputStream inputStream = getClass().getResourceAsStream(fileName);
            byte[] bytes = inputStream.readAllBytes();
            BufferedInputStream imageStream = new BufferedInputStream(new ByteArrayInputStream(bytes));

            int bufferSize = 1 * 1024;// 1K
            byte[] buffer = new byte[bufferSize];
            int length;
            while ((length = imageStream.read(buffer, 0, bufferSize)) != -1) {
                responseObserver.onNext(DataChunk.newBuilder()
                        .setData(ByteString.copyFrom(buffer, 0, length))
                        .setSize(bufferSize)
                        .build());
            }
            imageStream.close();
            responseObserver.onCompleted();
        } catch (Throwable e) {
            responseObserver.onError(Status.ABORTED
                    .withDescription("Unable to acquire the image " + request.getFileName())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public StreamObserver<UploadImageRequest> uploadImage(StreamObserver<UploadImageResponse> responseObserver) {
        return new StreamObserver<UploadImageRequest>() {
            private String laptopID;
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
                String imageID = "";
                int imageSize = imageData.size();

                try {
                    imageID = imageStore.Save(imageType, imageData);
                    Processing process = new Processing();
                    process.ProcessImage(imageID);
                } catch (IOException e) {
                    responseObserver.onError(
                            Status.INTERNAL
                                    .withDescription("cannot save the image to the store: " + e.getMessage())
                                    .asRuntimeException()
                    );
                }

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
