package org.xizhang.rpc.grpc.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ImageStore {
    String save(String laptopID, String imageType, ByteArrayOutputStream imageData) throws IOException;
}
