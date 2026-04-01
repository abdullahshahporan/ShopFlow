package com.shahporan.demo.security;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shahporan.demo.entity.Order;
import com.shahporan.demo.entity.Product;
import com.shahporan.demo.entity.Seller;
import com.shahporan.demo.entity.Stock;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.repository.OrderRepository;
import com.shahporan.demo.repository.ProductRepository;
import com.shahporan.demo.repository.SellerRepository;
import com.shahporan.demo.repository.StockRepository;
import com.shahporan.demo.repository.UserRepository;

@Configuration
public class DemoDataSeeder {

    @Bean
    public CommandLineRunner seedDemoData(
            UserRepository userRepository,
                        SellerRepository sellerRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            StockRepository stockRepository) {
        return args -> {
            // Get existing demo users
                        Seller seller = sellerRepository.findByEmailIgnoreCase("seller@demo.com").orElse(null);
            User buyer = userRepository.findByEmailIgnoreCase("buyer@demo.com").orElse(null);

            if (seller == null || buyer == null) {
                return; // Users should be seeded first by DemoUserSeederConfig
            }

            // Seed products if not already seeded
            if (productRepository.count() == 0) {
                                seedProducts(seller, productRepository, stockRepository);
            }

            // Seed orders if not already seeded
            if (orderRepository.count() == 0) {
                                seedOrders(buyer, seller, productRepository, orderRepository);
            }
        };
    }

    private void seedProducts(
            Seller seller,
            ProductRepository productRepository,
            StockRepository stockRepository) {
        
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

                // Create stock rows for products
        for (Product product : products) {
                        Stock stock = Stock.builder()
                    .product(product)
                    .seller(seller)
                                        .quantity(product.getQuantity())
                    .build();
                        stockRepository.save(stock);
        }
    }

    private void seedOrders(
            User buyer,
            Seller seller,
            ProductRepository productRepository,
            OrderRepository orderRepository) {
        
        // Get products
        List<Product> products = productRepository.findBySellerId(seller.getId());
        if (products.isEmpty()) {
            return;
        }

        // Create first order
        Order order1 = Order.builder()
                .buyer(buyer)
                .product(products.get(0)) // laptop
                .qty(1)
                .unitPrice(products.get(0).getPrice())
                .status("APPROVED")
                .total(products.get(0).getPrice())
                .build();

        orderRepository.save(order1);

        // Create second order
        Order order2 = Order.builder()
                .buyer(buyer)
                .product(products.get(3)) // monitor
                .qty(1)
                .unitPrice(products.get(3).getPrice())
                .status("PENDING")
                .total(products.get(3).getPrice())
                .build();
        orderRepository.save(order2);

    }
}
