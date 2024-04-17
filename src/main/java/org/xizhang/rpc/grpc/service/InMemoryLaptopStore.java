package org.xizhang.rpc.grpc.service;

import com.xizhang.rpc.grpc.pcbook.pb.Filter;
import com.xizhang.rpc.grpc.pcbook.pb.Laptop;
import com.xizhang.rpc.grpc.pcbook.pb.Memory;
import io.grpc.Context;

import java.nio.file.FileAlreadyExistsException;
import java.rmi.AlreadyBoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class InMemoryLaptopStore implements LaptopStore{
    private static final Logger logger = Logger.getLogger(InMemoryLaptopStore.class.getName());
    private ConcurrentMap<String, Laptop> data;

    public InMemoryLaptopStore(){
        data = new ConcurrentHashMap<>(0);//initiate the capacity to be 0
    }

    @Override
    public void save(Laptop laptop)  {
        if(data.containsKey(laptop.getId())){
            throw new AlreadyExistsException("LAPTOP id has already exists");
        }
        //deep copy
        Laptop laptopcopy = laptop.toBuilder().build();
        data.put(laptopcopy.getId(), laptopcopy);
    }

    @Override
    public Laptop find(String id) {
        if(!data.containsKey(id)){
            return null;
        }
        //deep copy
        Laptop laptopcopy = data.get(id).toBuilder().build();
        return laptopcopy;
    }

    @Override
    public void search(Context ctx, Filter filter, LaptopStream stream) {
        for(Map.Entry<String, Laptop> entry : data.entrySet()){
            if(ctx.isCancelled()){
                logger.info("context is cancelled");
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Laptop laptop = entry.getValue();
            if(isQualified(filter, laptop)){
                stream.send(laptop.toBuilder().build());
            }
        }
    }

    private boolean isQualified(Filter filter, Laptop laptop) {
        if(laptop.getPriceUsd() > filter.getMaxPriceUsd()){
            return false;
        }
        if(laptop.getCpu().getNumberCores() < filter.getMinCpuCores()){
            return false;
        }
        if(laptop.getCpu().getMinGhz() < filter.getMinCpuGhz()){
            return false;
        }
        if(toBit(laptop.getRam()) < toBit(filter.getMinRam())){
            return false;
        }
        return true;
    }

    private long toBit(Memory memory) {
        long value = memory.getValue();
        switch (memory.getUnit()){
            case BIT :
                return value;
            case BYTE:
                return value << 3;
            case KILOBYTE:
                return value << 13;
            case MEGABYTE:
                return value << 23;
            case GIGABYTE:
                return value << 33;
            case TERABYTE:
                return value << 43;
            default:
                return 0;
        }
    }
}
