package org.xizhang.rpc.grpc.service;

import com.google.protobuf.ByteString;
import com.xizhang.rpc.grpc.pcbook.pb.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.SslContext;
import org.xizhang.rpc.grpc.sample.Generator;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LaptopClient {
    private static final Logger logger = Logger.getLogger(LaptopClient.class.getName());

    private ManagedChannel channel; //connection bet client and server

    private LaptopServiceGrpc.LaptopServiceBlockingStub blockingStub;

    private LaptopServiceGrpc.LaptopServiceStub asyncStub;

    public LaptopClient(String host, int port){
        try{
            channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .build();
            blockingStub = LaptopServiceGrpc.newBlockingStub(channel);
            asyncStub = LaptopServiceGrpc.newStub(channel);
        }catch (UnsupportedOperationException e){
            e.getMessage();
        }

        //Security: As mentioned, .usePlaintext() disables encryption,
        // making the communication susceptible to eavesdropping.
        // For production environments, consider removing this call and setting up TLS to secure the channel.

    }

    public LaptopClient(String host, int port, SslContext sslContext){
        try{
            channel = NettyChannelBuilder.forAddress(host, port)
                    .sslContext(sslContext)
                    .build();
            blockingStub = LaptopServiceGrpc.newBlockingStub(channel);
            asyncStub = LaptopServiceGrpc.newStub(channel);
        }catch (UnsupportedOperationException e){
            e.getMessage();
        }

        //Security: As mentioned, .usePlaintext() disables encryption,
        // making the communication susceptible to eavesdropping.
        // For production environments, consider removing this call and setting up TLS to secure the channel.

    }

    public void shutdown() throws InterruptedException {

        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);

    }
    public void createLaptop(Laptop laptop){
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        CreateLaptopResponse response; // = CreateLaptopResponse.getDefaultInstance();
        try {
            System.out.println(request);
            response = blockingStub.createLaptop(request);
//            response = blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).createLaptop(request);
            System.out.println(response);
        }catch (StatusRuntimeException statusRuntimeException){
            if(statusRuntimeException.getStatus().getCode() == Status.Code.ALREADY_EXISTS){
                logger.info("laptop id already exists");
                return;
            }
            logger.log(Level.SEVERE, "request failed" + statusRuntimeException.getMessage());
            return;

        }catch (Exception e){
            logger.log(Level.SEVERE, "request failed" + e.getMessage());
            return;
        }
        logger.info("laptop created with ID" + response.getId());


    }

    private void searchLaptop(Filter filter) {
        logger.info("seatch started");

        SearchLaptopRequest request = SearchLaptopRequest.newBuilder().setFilter(filter).build();
        try{
            Iterator<SearchLaptopResponse> responseIterator = blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).searchLaptop(request);
            while (responseIterator.hasNext()){
                SearchLaptopResponse response = responseIterator.next();
                Laptop laptop = response.getLaptop();

                logger.info("found : " + laptop.getId());
            }
            logger.info("search completed");
        }catch (Exception e){
            logger.log(Level.SEVERE, "request failed " + e.getMessage());
            return;
        }
    }

    public void uploadImage(String laptopID, String imagePath) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1); // we only need to wait the response thread
        StreamObserver<UploadImageRequest> requestObserver = asyncStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .uploadImage(new StreamObserver<UploadImageResponse>() {
                    @Override
                    public void onNext(UploadImageResponse response) {
                        logger.info("receive response " + response);
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.info("upload failed");
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("image uploaded");
                        finishLatch.countDown();
                    }
                });
        FileInputStream fileInputStream;
        try  {
            fileInputStream = new FileInputStream(imagePath);
        }catch (Exception e){
            logger.log(Level.SEVERE, "cannot read image" + e.getMessage());
            return;
        }


        String imageType = imagePath.substring(imagePath.lastIndexOf("."));
        ImageInfo info = ImageInfo.newBuilder().setLaptopId(laptopID).setImageType(imageType).build();
        UploadImageRequest request = UploadImageRequest.newBuilder().setInfo(info).build();

        try{
            requestObserver.onNext(request);
            logger.info("sent image info: " + info);
            byte[] buffer = new byte[1024];
            while(true){
                int num = fileInputStream.read(buffer);
                if(num <= 0){
                    break;
                }

                if(finishLatch.getCount() == 0){
                    return;
                }

                request = UploadImageRequest.newBuilder()
                        .setChunkData(ByteString.copyFrom(buffer, 0, num))
                        .build();
                requestObserver.onNext(request);
                logger.log(Level.SEVERE, "chunk data was send with size " + num );
            }
        }catch (Exception e){
            logger.log(Level.SEVERE, "unexpected error" + e.getMessage());
            requestObserver.onError(e);
            return;
        }

        requestObserver.onCompleted();

        if(!finishLatch.await(1, TimeUnit.MINUTES)){
            logger.warning("timeout uploaded fail");
        }
    }

    public void rateLaptop(String[] laptopIDs, double[] scores) throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RateLaptopRequest> requestObserver = asyncStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .rateLaptop(new StreamObserver<RateLaptopResponse>() {
                    @Override
                    public void onNext(RateLaptopResponse response) {
                        logger.info("laptop rated : id = " + response.getLaptopId() +
                                " count " +
                                response.getRatedCount() +
                                ", average = " + response.getAverageScore());
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.log(Level.SEVERE, "rate laptop failed : " + t.getMessage());
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("rate laptop completed");
                    }
                });
        int n = laptopIDs.length;
        try{
            for (int cnt = 0; cnt < n; cnt++) {
                RateLaptopRequest request = RateLaptopRequest.newBuilder()
                        .setLaptopId(laptopIDs[cnt])
                        .setScore(scores[cnt])
                        .build();
                requestObserver.onNext(request);
                logger.info("sent rate-laptop request: id = " + request.getLaptopId() + ", score = " + request.getScore());
            }
        }catch (Exception e){
            logger.log(Level.SEVERE, "unexpected error" + e.getMessage());
            requestObserver.onError(e);
            return;
        }
        requestObserver.onCompleted();
        if(!finishLatch.await(1, TimeUnit.MINUTES)){
            logger.warning("cannot finished in 1 minute");
        }
    }
    public static void testCreateLaptop(LaptopClient client, Generator generator){
        Laptop laptop = generator.NewLaptop();
        client.createLaptop(laptop);
    }

    public static void testUploadImage(LaptopClient client, Generator generator){
        Laptop laptop = generator.NewLaptop();
        client.createLaptop(laptop);
        try {
            client.uploadImage(laptop.getId(), "tmp/laptop.jpg");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void testSearchLaptop(LaptopClient client, Generator generator){
        for(int i = 0; i < 10; i++){
                Laptop laptop = generator.NewLaptop();
                client.createLaptop(laptop);
            }
       Memory minRam = Memory.newBuilder()
                .setValue(8)
                .setUnit(Memory.Unit.GIGABYTE)
                .build();
        Filter filter = Filter.newBuilder()
                .setMaxPriceUsd(3000)
                .setMinCpuCores(4)
                .setMinCpuGhz(2.5)
                .setMinRam(minRam)
                .build();

        client.searchLaptop(filter);
    }

    public static void testRateLaptop(LaptopClient client, Generator generator) throws InterruptedException {
        int n = 3;
        String[] laptopIDS = new String[n];
        for (int i = 0; i < n; i++) {
            Laptop laptop = generator.NewLaptop();
            laptopIDS[i] = laptop.getId();
            client.createLaptop(laptop);
        }
        Scanner scanner = new Scanner(System.in);
        while(true){
            logger.info("rate laptop (y/n) ?");
            String answer = scanner.nextLine();
            if(answer.toLowerCase().trim().equals("n")){
                break;
            }
            double[] scores = new double[n];
            for (int i = 0; i < n; i++) {
                scores[i] = generator.NewLaptopScore();
            }
            client.rateLaptop(laptopIDS, scores);
        }
    }
    public static SslContext loadTLSCredentials() throws SSLException {
        File serverCACertFile = new File("cert/ca-cert.pem");
        File clientCertFile = new File("cert/client-cert.pem");
        File clientKeyFile = new File("cert/client-key.pem");
        return GrpcSslContexts.forClient()
                .keyManager(clientCertFile, clientKeyFile)
                .trustManager(serverCACertFile)
                .build();
    }

    public static void main(String[] args) throws InterruptedException, SSLException {
        SslContext sslContext = LaptopClient.loadTLSCredentials();

        LaptopClient client = new LaptopClient("0.0.0.0", 8080, sslContext);
        Generator generator = new Generator();

        try{
//            test create and search laptops
//            for(int i = 0; i < 10; i++){
//                Laptop laptop = generator.NewLaptop();
//                client.createLaptop(laptop);
//            }
//            Memory minRam = Memory.newBuilder()
//                    .setValue(8)
//                    .setUnit(Memory.Unit.GIGABYTE)
//                    .build();
//
//            Filter filter = Filter.newBuilder()
//                    .setMaxPriceUsd(3000)
//                    .setMinCpuCores(4)
//                    .setMinCpuGhz(2.5)
//                    .setMinRam(minRam)
//                    .build();
//
//            client.searchLaptop(filter);

//            test upload laptop image
//            Laptop laptop = generator.NewLaptop();
//            client.createLaptop(laptop);
//            client.uploadImage(laptop.getId(), "tmp/laptop.jpg");

            testRateLaptop(client, generator);

        }finally {
            client.shutdown();
        }
    }
}
