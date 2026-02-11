package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Order Controller Integration Tests")
class OrderControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Should return 404 when order does not exist")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        mockMvc.perform(post("/orders/{orderId}/processOrder", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should process order with normal product successfully")
    void shouldProcessOrderWithNormalProduct() throws Exception {
        
        Product product = createAndSaveProduct(10, 5, "NORMAL", "USB Cable", null, null, null);
        Order order = createAndSaveOrder(product);

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().toString()));

        Product updatedProduct = productRepository.findById(product.getId()).get();
        assertEquals(4, updatedProduct.getAvailable());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should notify delay when normal product out of stock")
    void shouldNotifyDelayWhenNormalProductOutOfStock() throws Exception {
        
        Product product = createAndSaveProduct(15, 0, "NORMAL", "USB Dongle", null, null, null);
        Order order = createAndSaveOrder(product);

        
        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        
        verify(notificationService, times(1)).sendDelayNotification(15, "USB Dongle");
    }

    @Test
    @DisplayName("Should process seasonal product in season")
    void shouldProcessSeasonalProductInSeason() throws Exception {
        
        LocalDate now = LocalDate.now();
        Product product = createAndSaveProduct(20, 5, "SEASONAL", "Watermelon", null,
                now.minusDays(10), now.plusDays(30));
        Order order = createAndSaveOrder(product);

        
        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        
        Product updatedProduct = productRepository.findById(product.getId()).get();
        assertEquals(4, updatedProduct.getAvailable());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should notify unavailability when seasonal product out of season")
    void shouldNotifyUnavailabilityWhenSeasonalProductOutOfSeason() throws Exception {
        
        LocalDate now = LocalDate.now();
        Product product = createAndSaveProduct(20, 5, "SEASONAL", "Grapes", null,
                now.plusDays(180), now.plusDays(240));
        Order order = createAndSaveOrder(product);

        
        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        
        verify(notificationService, times(1)).sendOutOfStockNotification("Grapes");
    }

    @Test
    @DisplayName("Should process expirable product not expired")
    void shouldProcessExpirableProductNotExpired() throws Exception {
        
        LocalDate now = LocalDate.now();
        Product product = createAndSaveProduct(10, 5, "EXPIRABLE", "Butter",
                now.plusDays(20), null, null);
        Order order = createAndSaveOrder(product);

        
        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        
        Product updatedProduct = productRepository.findById(product.getId()).get();
        assertEquals(4, updatedProduct.getAvailable());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should notify expiration when product expired")
    void shouldNotifyExpirationWhenExpired() throws Exception {
        
        LocalDate expiryDate = LocalDate.now().minusDays(2);
        Product product = createAndSaveProduct(10, 5, "EXPIRABLE", "Milk",
                expiryDate, null, null);
        Order order = createAndSaveOrder(product);

        
        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        
        verify(notificationService, times(1)).sendExpirationNotification("Milk", expiryDate);
    }

    @Test
    @DisplayName("Should process order with multiple products")
    void shouldProcessOrderWithMultipleProducts() throws Exception {
        
        LocalDate now = LocalDate.now();
        Product normalProduct = createAndSaveProduct(10, 5, "NORMAL", "Cable", null, null, null);
        Product seasonalProduct = createAndSaveProduct(20, 3, "SEASONAL", "Melon", null,
                now.minusDays(5), now.plusDays(20));
        Product expirableProduct = createAndSaveProduct(15, 2, "EXPIRABLE", "Milk",
                now.plusDays(10), null, null);

        Set<Product> products = new HashSet<>();
        products.add(normalProduct);
        products.add(seasonalProduct);
        products.add(expirableProduct);

        Order order = new Order();
        order.setItems(products);
        order = orderRepository.save(order);

        
        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk());

        
        assertEquals(4, productRepository.findById(normalProduct.getId()).get().getAvailable());
        assertEquals(2, productRepository.findById(seasonalProduct.getId()).get().getAvailable());
        assertEquals(1, productRepository.findById(expirableProduct.getId()).get().getAvailable());
        verifyNoInteractions(notificationService);
    }

    private Product createAndSaveProduct(int leadTime, int available, String type, String name,
                                         LocalDate expiryDate, LocalDate seasonStart, LocalDate seasonEnd) {
        Product product = new Product(null, leadTime, available, type, name,
                expiryDate, seasonStart, seasonEnd);
        return productRepository.save(product);
    }

    private Order createAndSaveOrder(Product... products) {
        Set<Product> productSet = new HashSet<>();
        for (Product p : products) {
            productSet.add(p);
        }
        Order order = new Order();
        order.setItems(productSet);
        return orderRepository.save(order);
    }
}