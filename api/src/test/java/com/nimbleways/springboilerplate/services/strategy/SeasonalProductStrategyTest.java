package com.nimbleways.springboilerplate.services.strategy;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Seasonal Product Strategy Tests")
class SeasonalProductStrategyTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    private SeasonalProductStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SeasonalProductStrategy(productRepository, notificationService);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should decrement stock when in season and available")
    void shouldDecrementStockWhenInSeasonAndAvailable() {
        
        LocalDate now = LocalDate.now();
        Product product = new Product(null, 20, 5, "SEASONAL", "Watermelon", null,
                now.minusDays(10), now.plusDays(30));

        
        strategy.process(product);

        
        assertEquals(4, product.getAvailable());
        verify(productRepository, times(1)).save(product);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should notify unavailability when before season starts")
    void shouldNotifyUnavailabilityWhenBeforeSeason() {
        
        LocalDate now = LocalDate.now();
        Product product = new Product(null, 20, 5, "SEASONAL", "Grapes", null,
                now.plusDays(180), now.plusDays(240));

        
        strategy.process(product);

        
        assertEquals(5, product.getAvailable());
        verify(notificationService, times(1)).sendOutOfStockNotification("Grapes");
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Should notify unavailability when after season ends")
    void shouldNotifyUnavailabilityWhenAfterSeason() {
        
        LocalDate now = LocalDate.now();
        Product product = new Product(null, 20, 5, "SEASONAL", "Strawberries", null,
                now.minusDays(100), now.minusDays(10));

        
        strategy.process(product);

        
        verify(notificationService, times(1)).sendOutOfStockNotification("Strawberries");
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Should notify delay when out of stock but delivery before season end")
    void shouldNotifyDelayWhenDeliveryBeforeSeasonEnd() {
        
        LocalDate now = LocalDate.now();
        Product product = new Product(null, 10, 0, "SEASONAL", "Peach", null,
                now.minusDays(10), now.plusDays(30));

        
        strategy.process(product);

        
        verify(notificationService, times(1)).sendDelayNotification(10, "Peach");
        verify(notificationService, never()).sendOutOfStockNotification(any());
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Should notify unavailability when out of stock and delivery after season end")
    void shouldNotifyUnavailabilityWhenDeliveryAfterSeasonEnd() {
        
        LocalDate now = LocalDate.now();
        Product product = new Product(null, 50, 0, "SEASONAL", "Mango", null,
                now.minusDays(10), now.plusDays(20));

        
        strategy.process(product);

        
        verify(notificationService, times(1)).sendOutOfStockNotification("Mango");
        verify(notificationService, never()).sendDelayNotification(anyInt(), any());
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Should handle season boundary - first day of season")
    void shouldHandleFirstDayOfSeason() {
        
        LocalDate now = LocalDate.now();
        Product product = new Product(null, 10, 5, "SEASONAL", "Cherry", null,
                now, now.plusDays(30));

        
        strategy.process(product);

        
        assertEquals(4, product.getAvailable());
        verify(productRepository, times(1)).save(product);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should handle season boundary - last day of season")
    void shouldHandleLastDayOfSeason() {
        
        LocalDate now = LocalDate.now();
        Product product = new Product(null, 10, 5, "SEASONAL", "Plum", null,
                now.minusDays(30), now);

        
        strategy.process(product);

        
        verify(notificationService, times(1)).sendOutOfStockNotification("Plum");
        verify(productRepository, never()).save(product);
    }
}