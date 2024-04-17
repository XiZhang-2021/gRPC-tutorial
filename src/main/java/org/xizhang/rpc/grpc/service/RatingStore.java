package org.xizhang.rpc.grpc.service;

public interface RatingStore {
    Rating add(String laptopID, double score);
}
