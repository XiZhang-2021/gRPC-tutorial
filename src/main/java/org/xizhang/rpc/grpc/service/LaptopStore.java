package org.xizhang.rpc.grpc.service;

import com.xizhang.rpc.grpc.pcbook.pb.Filter;
import com.xizhang.rpc.grpc.pcbook.pb.Laptop;
import io.grpc.Context;

public interface LaptopStore {
    void save(Laptop laptop);

    Laptop find(String id);

    void search(Context ctx, Filter filter, LaptopStream stream);
}

