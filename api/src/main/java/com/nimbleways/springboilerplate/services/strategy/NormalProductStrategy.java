package com.nimbleways.springboilerplate.services.strategy;

import com.nimbleways.springboilerplate.domain.strategy.ProductProcessingStrategy;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.springframework.stereotype.Component;

@Component("NORMAL")
public class NormalProductStrategy implements ProductProcessingStrategy {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public NormalProductStrategy(ProductRepository productRepository,
                                 NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void process(Product product) {
        if (product.getAvailable() > 0) {
            decrementStock(product);
        } else {
            notifyDelay(product);
        }
    }

    private void decrementStock(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }

    private void notifyDelay(Product product) {
        notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
    }
}
