package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.domain.strategy.ProductProcessingStrategy;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class OrderProcessingService {

    private final OrderRepository orderRepository;
    private final Map<String, ProductProcessingStrategy> strategies;

    public OrderProcessingService(OrderRepository orderRepository,
                                  Map<String, ProductProcessingStrategy> strategies) {
        this.orderRepository = orderRepository;
        this.strategies = strategies;
    }

    @Transactional
    public Order processOrder(Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.getItems().forEach(this::processProduct);

        return order;
    }

    private void processProduct(Product product) {
        var strategyName = product.getType();
        var strategy = strategies.get(strategyName);

        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for product type: " + strategyName);
        }

        strategy.process(product);
    }
}