package com.nimbleways.springboilerplate.services.strategy;

import com.nimbleways.springboilerplate.domain.strategy.ProductProcessingStrategy;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component("SEASONAL")
public class SeasonalProductStrategy implements ProductProcessingStrategy {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public SeasonalProductStrategy(ProductRepository productRepository,
                                   NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void process(Product product) {
        var now = LocalDate.now();
        var seasonStart = product.getSeasonStartDate();
        var seasonEnd = product.getSeasonEndDate();

        if (isOutOfSeason(now, seasonStart, seasonEnd)) {
            notificationService.sendOutOfStockNotification(product.getName());
            return;
        }

        if (product.getAvailable() > 0) {
            decrementStock(product);
            return;
        }

        handleOutOfStock(product, now, seasonEnd);
    }

    private boolean isOutOfSeason(LocalDate now, LocalDate seasonStart, LocalDate seasonEnd) {
        return now.isBefore(seasonStart) || !now.isBefore(seasonEnd);
    }

    private void handleOutOfStock(Product product, LocalDate now, LocalDate seasonEnd) {
        var deliveryDate = now.plusDays(product.getLeadTime());

        if (deliveryDate.isBefore(seasonEnd)) {
            notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
        } else {
            notificationService.sendOutOfStockNotification(product.getName());
        }
    }

    private void decrementStock(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}
