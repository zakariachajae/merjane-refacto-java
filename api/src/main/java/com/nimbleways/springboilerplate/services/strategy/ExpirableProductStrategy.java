package com.nimbleways.springboilerplate.services.strategy;

import com.nimbleways.springboilerplate.domain.strategy.ProductProcessingStrategy;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component("EXPIRABLE")
public class ExpirableProductStrategy implements ProductProcessingStrategy {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public ExpirableProductStrategy(ProductRepository productRepository,
                                    NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void process(Product product) {
        var now = LocalDate.now();
        var expiryDate = product.getExpiryDate();

        if (isExpired(expiryDate, now)) {
            notificationService.sendExpirationNotification(product.getName(), expiryDate);
            return;
        }

        if (product.getAvailable() > 0) {
            decrementStock(product);
            return;
        }

        handleOutOfStock(product, now, expiryDate);
    }

    private boolean isExpired(LocalDate expiryDate, LocalDate now) {
        return !expiryDate.isAfter(now);
    }

    private void handleOutOfStock(Product product, LocalDate now, LocalDate expiryDate) {
        var deliveryDate = now.plusDays(product.getLeadTime());

        if (deliveryDate.isBefore(expiryDate)) {
            notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
        } else {
            notificationService.sendExpirationNotification(product.getName(), expiryDate);
        }
    }

    private void decrementStock(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}
