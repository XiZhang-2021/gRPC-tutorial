package org.xizhang.rpc.grpc.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryRatingStore implements RatingStore{
    private ConcurrentMap<String, Rating> data;

    public InMemoryRatingStore(){
        data = new ConcurrentHashMap<>(0);
    }

    @Override
    public Rating add(String laptopID, double score) {
        return data.merge(laptopID, new Rating(1, score), Rating::add);
    }


}
