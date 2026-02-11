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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Normal Product Strategy Tests")
class NormalProductStrategyTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    private NormalProductStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new NormalProductStrategy(productRepository, notificationService);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should decrement stock when product has available stock")
    void shouldDecrementStockWhenAvailable() {
        
        Product product = new Product(null, 10, 5, "NORMAL", "USB Cable", null, null, null);

        
        strategy.process(product);

        
        assertEquals(4, product.getAvailable());
        verify(productRepository, times(1)).save(product);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should notify delay when product is out of stock")
    void shouldNotifyDelayWhenOutOfStock() {
        
        Product product = new Product(null, 15, 0, "NORMAL", "USB Dongle", null, null, null);

        
        strategy.process(product);

        
        assertEquals(0, product.getAvailable());
        verify(notificationService, times(1)).sendDelayNotification(15, "USB Dongle");
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Should decrement stock from multiple units")
    void shouldDecrementMultipleStock() {
        
        Product product = new Product(null, 20, 100, "NORMAL", "HDMI Cable", null, null, null);

        
        strategy.process(product);

        
        assertEquals(99, product.getAvailable());
        verify(productRepository, times(1)).save(product);
    }
}
