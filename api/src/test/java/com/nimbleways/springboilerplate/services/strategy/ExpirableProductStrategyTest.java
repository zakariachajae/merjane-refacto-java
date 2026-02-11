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
@DisplayName("Expirable Product Strategy Tests")
class ExpirableProductStrategyTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    private ExpirableProductStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ExpirableProductStrategy(productRepository, notificationService);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should decrement stock when not expired and available")
    void shouldDecrementStockWhenNotExpiredAndAvailable() {
        
        LocalDate now = LocalDate.now();
        Product product = new Product(null, 10, 5, "EXPIRABLE", "Butter",
                now.plusDays(20), null, null);

        
        strategy.process(product);

        
        assertEquals(4, product.getAvailable());
        verify(productRepository, times(1)).save(product);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should notify expiration when product is expired")
    void shouldNotifyExpirationWhenExpired() {
        
        LocalDate expiryDate = LocalDate.now().minusDays(2);
        Product product = new Product(null, 10, 5, "EXPIRABLE", "Milk",
                expiryDate, null, null);

        
        strategy.process(product);

        
        assertEquals(5, product.getAvailable());
        verify(notificationService, times(1)).sendExpirationNotification("Milk", expiryDate);
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Should notify expiration when product expires today")
    void shouldNotifyExpirationWhenExpiringToday() {
        
        LocalDate today = LocalDate.now();
        Product product = new Product(null, 10, 5, "EXPIRABLE", "Yogurt",
                today, null, null);

        
        strategy.process(product);

        
        verify(notificationService, times(1)).sendExpirationNotification("Yogurt", today);
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Should notify delay when out of stock but delivery before expiry")
    void shouldNotifyDelayWhenDeliveryBeforeExpiry() {
        
        LocalDate now = LocalDate.now();
        Product product = new Product(null, 10, 0, "EXPIRABLE", "Cheese",
                now.plusDays(20), null, null);

        
        strategy.process(product);

        
        verify(notificationService, times(1)).sendDelayNotification(10, "Cheese");
        verify(notificationService, never()).sendExpirationNotification(any(), any());
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Should notify expiration when out of stock and delivery after expiry")
    void shouldNotifyExpirationWhenDeliveryAfterExpiry() {
        
        LocalDate expiryDate = LocalDate.now().plusDays(5);
        Product product = new Product(null, 10, 0, "EXPIRABLE", "Cream",
                expiryDate, null, null);

        
        strategy.process(product);

        
        verify(notificationService, times(1)).sendExpirationNotification("Cream", expiryDate);
        verify(notificationService, never()).sendDelayNotification(anyInt(), any());
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Should handle expiry boundary - expires tomorrow")
    void shouldHandleExpiresTomorrow() {
        
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Product product = new Product(null, 10, 5, "EXPIRABLE", "Eggs",
                tomorrow, null, null);

        
        strategy.process(product);

        
        assertEquals(4, product.getAvailable());
        verify(productRepository, times(1)).save(product);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should handle long shelf life product")
    void shouldHandleLongShelfLife() {
        
        LocalDate futureDate = LocalDate.now().plusYears(2);
        Product product = new Product(null, 30, 100, "EXPIRABLE", "Canned Food",
                futureDate, null, null);

        
        strategy.process(product);

        
        assertEquals(99, product.getAvailable());
        verify(productRepository, times(1)).save(product);
        verifyNoInteractions(notificationService);
    }
}