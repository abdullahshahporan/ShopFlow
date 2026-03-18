package com.shahporan.demo.security;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shahporan.demo.entity.Order;
import com.shahporan.demo.entity.OrderItem;
import com.shahporan.demo.entity.Product;
import com.shahporan.demo.entity.StockMovement;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.repository.OrderItemRepository;
import com.shahporan.demo.repository.OrderRepository;
import com.shahporan.demo.repository.ProductRepository;
import com.shahporan.demo.repository.StockMovementRepository;
import com.shahporan.demo.repository.UserRepository;

@Configuration
public class DemoDataSeeder {

    @Bean
    public CommandLineRunner seedDemoData(
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            StockMovementRepository stockMovementRepository) {
        return args -> {
            // Get existing demo users
            User seller = userRepository.findByEmailIgnoreCase("seller@demo.com").orElse(null);
            User buyer = userRepository.findByEmailIgnoreCase("buyer@demo.com").orElse(null);

            if (seller == null || buyer == null) {
                return; // Users should be seeded first by DemoUserSeederConfig
            }

            // Seed products if not already seeded
            if (productRepository.count() == 0) {
                seedProducts(seller, productRepository, stockMovementRepository);
            }

            // Seed orders if not already seeded
            if (orderRepository.count() == 0) {
                seedOrders(buyer, seller, productRepository, orderRepository, orderItemRepository);
            }
        };
    }

    private void seedProducts(
            User seller,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository) {
        
        // Create sample products
        Product laptop = Product.builder()
                .seller(seller)
                .name("Dell XPS 13")
                .sku("DELL-XPS-13-001")
                .price(new BigDecimal("999.99"))
                .quantity(15)
                .active(true)
                .build();

        Product mouse = Product.builder()
                .seller(seller)
                .name("Logitech MX Master 3")
                .sku("LOG-MX-MASTER-3")
                .price(new BigDecimal("99.99"))
                .quantity(50)
                .active(true)
                .build();

        Product keyboard = Product.builder()
                .seller(seller)
                .name("Mechanical Keyboard RGB")
                .sku("MECH-KB-RGB-001")
                .price(new BigDecimal("129.99"))
                .quantity(30)
                .active(true)
                .build();

        Product monitor = Product.builder()
                .seller(seller)
                .name("LG UltraWide Monitor 34\"")
                .sku("LG-ULTRA-34")
                .price(new BigDecimal("499.99"))
                .quantity(10)
                .active(true)
                .build();

        Product headphones = Product.builder()
                .seller(seller)
                .name("Sony WH-1000XM5 Headphones")
                .sku("SONY-WH-1000XM5")
                .price(new BigDecimal("349.99"))
                .quantity(25)
                .active(true)
                .build();

        Product usb_hub = Product.builder()
                .seller(seller)
                .name("USB-C Hub 7-in-1")
                .sku("USB-HUB-7-001")
                .price(new BigDecimal("49.99"))
                .quantity(40)
                .active(true)
                .build();

        Product webcam = Product.builder()
                .seller(seller)
                .name("Logitech 4K Webcam")
                .sku("LOG-4K-CAM-001")
                .price(new BigDecimal("79.99"))
                .quantity(20)
                .active(true)
                .build();

        Product mousepad = Product.builder()
                .seller(seller)
                .name("Extended Gaming Mousepad")
                .sku("GAME-PAD-EXT-001")
                .price(new BigDecimal("24.99"))
                .quantity(60)
                .active(true)
                .build();

        // Save products
        List<Product> products = productRepository.saveAll(
                List.of(laptop, mouse, keyboard, monitor, headphones, usb_hub, webcam, mousepad)
        );

        // Create stock movements for products
        for (Product product : products) {
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .seller(seller)
                    .type(StockMovement.MovementType.IN)
                    .qty(product.getQuantity())
                    .note("Initial stock")
                    .build();
            stockMovementRepository.save(movement);
        }
    }

    private void seedOrders(
            User buyer,
            User seller,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {
        
        // Get products
        List<Product> products = productRepository.findBySellerId(seller.getId());
        if (products.isEmpty()) {
            return;
        }

        // Create first order with multiple items
        Order order1 = Order.builder()
                .buyer(buyer)
                .status("CONFIRMED")
                .total(BigDecimal.ZERO)
                .build();

        order1 = orderRepository.save(order1);

        // Add items to order1
        OrderItem item1 = OrderItem.builder()
                .order(order1)
                .product(products.get(0)) // laptop
                .qty(1)
                .unitPrice(products.get(0).getPrice())
                .build();

        OrderItem item2 = OrderItem.builder()
                .order(order1)
                .product(products.get(1)) // mouse
                .qty(2)
                .unitPrice(products.get(1).getPrice())
                .build();

        OrderItem item3 = OrderItem.builder()
                .order(order1)
                .product(products.get(2)) // keyboard
                .qty(1)
                .unitPrice(products.get(2).getPrice())
                .build();

        orderItemRepository.saveAll(List.of(item1, item2, item3));

        order1.setItems(List.of(item1, item2, item3));
        BigDecimal total1 = products.get(0).getPrice()
                .add(products.get(1).getPrice().multiply(new BigDecimal("2")))
                .add(products.get(2).getPrice());
        order1.setTotal(total1);
        orderRepository.save(order1);

        // Create second order
        Order order2 = Order.builder()
                .buyer(buyer)
                .status("PENDING")
                .total(BigDecimal.ZERO)
                .build();

        order2 = orderRepository.save(order2);

        OrderItem item4 = OrderItem.builder()
                .order(order2)
                .product(products.get(3)) // monitor
                .qty(1)
                .unitPrice(products.get(3).getPrice())
                .build();

        OrderItem item5 = OrderItem.builder()
                .order(order2)
                .product(products.get(4)) // headphones
                .qty(1)
                .unitPrice(products.get(4).getPrice())
                .build();

        orderItemRepository.saveAll(List.of(item4, item5));

        order2.setItems(List.of(item4, item5));
        BigDecimal total2 = products.get(3).getPrice()
                .add(products.get(4).getPrice());
        order2.setTotal(total2);
        orderRepository.save(order2);

        // Create third order
        Order order3 = Order.builder()
                .buyer(buyer)
                .status("CANCELLED")
                .total(BigDecimal.ZERO)
                .build();

        order3 = orderRepository.save(order3);

        OrderItem item6 = OrderItem.builder()
                .order(order3)
                .product(products.get(5)) // usb_hub
                .qty(3)
                .unitPrice(products.get(5).getPrice())
                .build();

        orderItemRepository.save(item6);

        order3.setItems(List.of(item6));
        order3.setTotal(products.get(5).getPrice().multiply(new BigDecimal("3")));
        orderRepository.save(order3);
    }
}
