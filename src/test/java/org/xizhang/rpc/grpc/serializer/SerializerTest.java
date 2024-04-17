package org.xizhang.rpc.grpc.serializer;

import com.xizhang.rpc.grpc.pcbook.pb.Laptop;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.xizhang.rpc.grpc.sample.Generator;

class SerializerTest {

    @Test
    void writeAndReadBinaryFile() {
        String binaryFile = "laptop.bin";
        Laptop laptop1 = new Generator().NewLaptop();
        Serializer serializer = new Serializer();
        serializer.writeBinaryFile(laptop1, binaryFile);
        Laptop laptop2 = serializer.readBinaryFile(binaryFile);
        Assert.assertEquals(laptop1, laptop2);
    }
}