package org.xizhang.rpc.grpc.service;

import com.xizhang.rpc.grpc.pcbook.pb.Laptop;

public interface LaptopStream{
    void send(Laptop laptop);
}
