package org.xizhang.rpc.grpc.service;

import com.google.protobuf.ByteString;
import com.xizhang.rpc.grpc.pcbook.pb.*;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LaptopService extends LaptopServiceGrpc.LaptopServiceImplBase{
    private static final Logger logger = Logger.getLogger(LaptopService.class.getName());
    private LaptopStore laptopStore;
    private ImageStore imageStore;
    private RatingStore ratingStore;

    public LaptopService(LaptopStore laptopStore, ImageStore imageStore, RatingStore ratingStore){
        this.laptopStore = laptopStore;
        this.imageStore = imageStore;
        this.ratingStore = ratingStore;
    }

    @Override
    public void createLaptop(CreateLaptopRequest request, StreamObserver<CreateLaptopResponse> responseObserver) {
        Laptop laptop = request.getLaptop();

        String id = laptop.getId();
        logger.info("got a create-laptop request with ID: " + id);
        UUID uuid;
        if(id.isEmpty()){
            uuid = UUID.randomUUID();
        }else{
            try{
                uuid = UUID.fromString(id);
            }catch (IllegalArgumentException e){
                responseObserver.onError(
                        Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException()
                );
                return;
            }
        }

//        try {
//            TimeUnit.SECONDS.sleep(6);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        if(Context.current().isCancelled()){
            logger.info("request is cancelled");
            responseObserver.onError(
                    Status.CANCELLED.withDescription("request is cancelled").asRuntimeException()
            );
            return;
        }

        Laptop laptopcopy = laptop.toBuilder().setId(uuid.toString()).build();
        try{
            this.laptopStore.save(laptopcopy);
        }catch(AlreadyExistsException alreadyExistsException){
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription(
                            alreadyExistsException.getMessage()
                    ).asRuntimeException());
            return;
        }catch (Exception e){
            responseObserver.onError(Status.INTERNAL
                    .withDescription(
                            e.getMessage()
                    ).asRuntimeException());
            return;
        }
        CreateLaptopResponse response = CreateLaptopResponse.newBuilder().setId(laptopcopy.getId()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        //This line signals that the server has finished sending messages to the client.
        //For unary RPCs, it effectively marks the end of the call from the server's side.
        //After calling .onCompleted(), no more messages can be sent to the client using this responseObserver.

        logger.info("save laptop with id : " + laptopcopy.getId());
    }

    @Override
    public void searchLaptop(SearchLaptopRequest request, StreamObserver<SearchLaptopResponse> responseObserver){
        Filter filter = request.getFilter();
        logger.info("got a filter from this request with filter " + filter);

        this.laptopStore.search(Context.current(), filter, new LaptopStream() {
            @Override
            public void send(Laptop laptop) {
                logger.info("found laptop with ID : " + laptop.getId());
                SearchLaptopResponse response = SearchLaptopResponse.newBuilder().setLaptop(laptop).build();
                responseObserver.onNext(response);
            }
        });

        responseObserver.onCompleted();
        logger.info("search laptop completed");
    }

    @Override
    public StreamObserver<UploadImageRequest> uploadImage(StreamObserver<UploadImageResponse> responseObserver){
        return new StreamObserver<UploadImageRequest>() {
            private String laptopID;
            private String imageType;
            private ByteArrayOutputStream imageData;
            private static final int maxImageSize = 1 << 20;
            @Override
            public void onNext(UploadImageRequest request) {
                if(request.getDataCase() == UploadImageRequest.DataCase.INFO){
                    ImageInfo info = request.getInfo();
                    logger.info("receive image info : " + info);

                    laptopID = info.getLaptopId();
                    imageType = info.getImageType();
                    imageData = new ByteArrayOutputStream();

                    // check laptop exists in store
                    Laptop found = laptopStore.find(laptopID);
                    if(found == null){
                        responseObserver.onError(
                                Status.NOT_FOUND.withDescription("laptop id does not exists")
                                        .asRuntimeException()
                        );
                    }

                    return;
                }
                ByteString chunkData = request.getChunkData();
                logger.info("receive image chunk with size + " + chunkData.size());
                if(imageData == null){
                    logger.info("image info wasn't sent before");
                    responseObserver.onError(Status.INVALID_ARGUMENT
                            .withDescription("image wasn't sent before").asRuntimeException());
                    return;
                }
                int size = imageData.size() + chunkData.size();
                if(size > maxImageSize){
                    logger.info("image is too large");
                    responseObserver.onError(Status.INVALID_ARGUMENT
                            .withDescription("image was too large").asRuntimeException());
                    return;
                }
                try {
                    chunkData.writeTo(imageData); //write data
                } catch (IOException e) {
                    responseObserver.onError(Status.INTERNAL
                            .withDescription("cannot write chunk data " + e.getMessage()).asRuntimeException());
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
                    imageID = imageStore.save(laptopID, imageType, imageData);
                } catch (IOException e) {
                    responseObserver.onError(Status.INTERNAL
                            .withDescription("cannot save image to store " + e.getMessage()).asRuntimeException());
                    return;
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
    @Override
    public StreamObserver<RateLaptopRequest> rateLaptop(StreamObserver<RateLaptopResponse> responseObserver){
        return new StreamObserver<RateLaptopRequest>() {
            @Override
            public void onNext(RateLaptopRequest request) {
                String laptopID = request.getLaptopId();
                double score = request.getScore();

                logger.info("received rate-laptop request : id = " + laptopID + " score " + score );
                Laptop found = laptopStore.find(laptopID);
                if(found == null){
                    responseObserver.onError(Status.NOT_FOUND
                            .withDescription("laptop not found ").asRuntimeException());
                    return;
                }
                Rating rating = ratingStore.add(laptopID, score);
                RateLaptopResponse response = RateLaptopResponse.newBuilder()
                        .setLaptopId(laptopID)
                        .setRatedCount(rating.getCount())
                        .setAverageScore(rating.getSum()/rating.getCount())
                        .build();
                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable t) {
                logger.warning(t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
