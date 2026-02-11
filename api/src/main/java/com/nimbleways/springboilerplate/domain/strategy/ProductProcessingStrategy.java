package com.nimbleways.springboilerplate.domain.strategy;

import com.nimbleways.springboilerplate.entities.Product;

public interface ProductProcessingStrategy {
    void process(Product product);
}
