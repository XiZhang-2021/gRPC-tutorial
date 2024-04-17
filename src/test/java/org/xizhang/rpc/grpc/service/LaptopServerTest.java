package org.xizhang.rpc.grpc.service;

import com.xizhang.rpc.grpc.pcbook.pb.*;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.After;
import org.junit.Rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xizhang.rpc.grpc.sample.Generator;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LaptopServerTest {
    private LaptopStore laptopStore;
    private LaptopServer server;
    private ManagedChannel channel;
    private ImageStore imageStore;
    private RatingStore ratingStore;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule(); // shutdow the channel at the end of the test

    @BeforeEach
    public void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName).directExecutor();
        laptopStore = new InMemoryLaptopStore();
        ratingStore = new InMemoryRatingStore();
        imageStore = new DiskImageStore("img");
        server = new LaptopServer(serverBuilder, 0, laptopStore, imageStore, ratingStore);
        server.start();
        channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build()
        );
        System.out.println(channel);
    }

    @After
    public void tearDown() throws InterruptedException {
        server.stop();
    }

    @Test
    public void createLaptopWithAValidID() {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop();
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        CreateLaptopResponse response = stub.createLaptop(request);
        assertNotNull(response);
        assertEquals(laptop.getId(), response.getId());
        Laptop found = laptopStore.find(response.getId());
        assertNotNull(found);
    }

    @Test
    public void createLaptopWithAnEmptyID() {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop().toBuilder().setId("").build();
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        CreateLaptopResponse response = stub.createLaptop(request);
        assertNotNull(response);
        assertFalse(response.getId().isEmpty());
        Laptop found = laptopStore.find(response.getId());
        assertNotNull(found);
    }

    @Test
    public void createLaptopWithAnInvalidID() {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop().toBuilder().setId("Invalid").build();
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        assertThrows(StatusRuntimeException.class, () -> {
            CreateLaptopResponse response = stub.createLaptop(request);
            assertNotNull(response);
            assertNull(response.getId());
        });

    }

    @Test
    public void createLaptopWithAnAlreadyExistsID() {
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop();
        laptopStore.save(laptop);
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        assertThrows(StatusRuntimeException.class, () -> {
            CreateLaptopResponse response = stub.createLaptop(request);
        });

    }

    @Test
    public void rateLaptop(){
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop();
        laptopStore.save(laptop);
        LaptopServiceGrpc.LaptopServiceStub stub = LaptopServiceGrpc.newStub(channel);
        RateLaptopResponseStreamObserver responseObserver = new RateLaptopResponseStreamObserver();

        StreamObserver<RateLaptopRequest> requestObserver = stub.rateLaptop(responseObserver);
        double[] scores = new double[]{8, 7.5, 10};
        double[] averages = {8, 7.75, 8.5};
        int n = scores.length;
        for (int i = 0; i < n; i++) {
            RateLaptopRequest request = RateLaptopRequest.newBuilder()
                    .setLaptopId(laptop.getId())
                    .setScore(scores[i])
                    .build();
            requestObserver.onNext(request);
        }
        requestObserver.onCompleted();
        assertNull(responseObserver.err);
        assertTrue(responseObserver.completed);
        assertEquals(n, responseObserver.responseList.size());
        int idx = 0;
        for(RateLaptopResponse response : responseObserver.responseList){
            assertEquals(laptop.getId(), response.getLaptopId());
            assertEquals(idx+1, response.getRatedCount());
            assertEquals(averages[idx], response.getAverageScore(), 1e-9);
            idx++;
        }
    }
    private class RateLaptopResponseStreamObserver implements StreamObserver<RateLaptopResponse>{
        public List<RateLaptopResponse> responseList;
        public Throwable err;
        public boolean completed;

        public RateLaptopResponseStreamObserver() {
            this.responseList = new LinkedList<>();

        }

        @Override
        public void onNext(RateLaptopResponse response) {
            responseList.add(response);
        }

        @Override
        public void onError(Throwable throwable) {
            err = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}