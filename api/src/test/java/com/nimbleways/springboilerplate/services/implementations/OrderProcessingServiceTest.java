package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.domain.strategy.ProductProcessingStrategy;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Processing Service Tests")
class OrderProcessingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductProcessingStrategy normalStrategy;

    @Mock
    private ProductProcessingStrategy seasonalStrategy;

    @Mock
    private ProductProcessingStrategy expirableStrategy;

    private OrderProcessingService service;

    @BeforeEach
    void setUp() {
        Map<String, ProductProcessingStrategy> strategies = new HashMap<>();
        strategies.put("NORMAL", normalStrategy);
        strategies.put("SEASONAL", seasonalStrategy);
        strategies.put("EXPIRABLE", expirableStrategy);

        service = new OrderProcessingService(orderRepository, strategies);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order does not exist")
    void shouldThrowOrderNotFoundExceptionWhenOrderDoesNotExist() {
        
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());


        assertThrows(OrderNotFoundException.class, () -> service.processOrder(orderId));
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("Should process order with normal product")
    void shouldProcessOrderWithNormalProduct() {
        
        Long orderId = 1L;
        Product normalProduct = new Product(null, 10, 5, "NORMAL", "Cable", null, null, null);
        Set<Product> products = new HashSet<>();
        products.add(normalProduct);

        Order order = new Order();
        order.setId(orderId);
        order.setItems(products);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        
        Order result = service.processOrder(orderId);

        
        assertNotNull(result);
        assertEquals(orderId, result.getId());
        verify(normalStrategy, times(1)).process(normalProduct);
        verifyNoInteractions(seasonalStrategy, expirableStrategy);
    }

    @Test
    @DisplayName("Should process order with seasonal product")
    void shouldProcessOrderWithSeasonalProduct() {
        
        Long orderId = 2L;
        LocalDate now = LocalDate.now();
        Product seasonalProduct = new Product(null, 20, 3, "SEASONAL", "Watermelon", null,
                now.minusDays(10), now.plusDays(30));
        Set<Product> products = new HashSet<>();
        products.add(seasonalProduct);

        Order order = new Order();
        order.setId(orderId);
        order.setItems(products);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        
        Order result = service.processOrder(orderId);

        
        assertNotNull(result);
        verify(seasonalStrategy, times(1)).process(seasonalProduct);
        verifyNoInteractions(normalStrategy, expirableStrategy);
    }

    @Test
    @DisplayName("Should process order with expirable product")
    void shouldProcessOrderWithExpirableProduct() {
        
        Long orderId = 3L;
        LocalDate now = LocalDate.now();
        Product expirableProduct = new Product(null, 15, 2, "EXPIRABLE", "Milk",
                now.plusDays(10), null, null);
        Set<Product> products = new HashSet<>();
        products.add(expirableProduct);

        Order order = new Order();
        order.setId(orderId);
        order.setItems(products);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        
        Order result = service.processOrder(orderId);

        
        assertNotNull(result);
        verify(expirableStrategy, times(1)).process(expirableProduct);
        verifyNoInteractions(normalStrategy, seasonalStrategy);
    }

    @Test
    @DisplayName("Should process order with multiple products of different types")
    void shouldProcessOrderWithMultipleProducts() {
        
        Long orderId = 4L;
        LocalDate now = LocalDate.now();

        Product normalProduct = new Product(null, 10, 5, "NORMAL", "Cable", null, null, null);
        Product seasonalProduct = new Product(null, 20, 3, "SEASONAL", "Melon", null,
                now.minusDays(5), now.plusDays(20));
        Product expirableProduct = new Product(null, 15, 2, "EXPIRABLE", "Milk",
                now.plusDays(10), null, null);

        Set<Product> products = new HashSet<>();
        products.add(normalProduct);
        products.add(seasonalProduct);
        products.add(expirableProduct);

        Order order = new Order();
        order.setId(orderId);
        order.setItems(products);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        
        Order result = service.processOrder(orderId);

        
        assertNotNull(result);
        verify(normalStrategy, times(1)).process(normalProduct);
        verify(seasonalStrategy, times(1)).process(seasonalProduct);
        verify(expirableStrategy, times(1)).process(expirableProduct);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when strategy not found for product type")
    void shouldThrowIllegalArgumentExceptionWhenStrategyNotFound() {
        
        Long orderId = 5L;
        Product unknownProduct = new Product(null, 10, 5, "UNKNOWN", "Mystery", null, null, null);
        Set<Product> products = new HashSet<>();
        products.add(unknownProduct);

        Order order = new Order();
        order.setId(orderId);
        order.setItems(products);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> service.processOrder(orderId));
    }

    @Test
    @DisplayName("Should handle empty order")
    void shouldHandleEmptyOrder() {
        
        Long orderId = 6L;
        Order order = new Order();
        order.setId(orderId);
        order.setItems(new HashSet<>());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        
        Order result = service.processOrder(orderId);

        
        assertNotNull(result);
        verifyNoInteractions(normalStrategy, seasonalStrategy, expirableStrategy);
    }
}