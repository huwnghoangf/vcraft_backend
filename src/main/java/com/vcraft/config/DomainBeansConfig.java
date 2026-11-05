package com.vcraft.config;

import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.BatchRepository;
import com.vcraft.domain.repository.BidRepository;
import com.vcraft.domain.repository.CartRepository;
import com.vcraft.domain.repository.CategoryRepository;
import com.vcraft.domain.repository.NewsRepository;
import com.vcraft.domain.repository.OrderRepository;
import com.vcraft.domain.repository.ProductRepository;
import com.vcraft.domain.repository.ProductionLogRepository;
import com.vcraft.domain.repository.QuoteRepository;
import com.vcraft.domain.repository.RefreshTokenRepository;
import com.vcraft.domain.repository.ReviewRepository;
import com.vcraft.domain.repository.UserRepository;
import com.vcraft.domain.service.AdminService;
import com.vcraft.domain.service.ArtisanService;
import com.vcraft.domain.service.AuthService;
import com.vcraft.domain.service.BatchService;
import com.vcraft.domain.service.CartService;
import com.vcraft.domain.service.CategoryService;
import com.vcraft.domain.service.NewsService;
import com.vcraft.domain.service.OrderService;
import com.vcraft.domain.service.PasswordHasher;
import com.vcraft.domain.service.ProductService;
import com.vcraft.domain.service.QrCodeGenerator;
import com.vcraft.domain.service.QuoteService;
import com.vcraft.domain.service.ReviewService;
import com.vcraft.domain.service.TokenIssuer;
import com.vcraft.domain.service.TraceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nơi khởi tạo các Service của tầng Domain thành Spring Bean.
 * Giữ tầng Domain hoàn toàn là Java thuần túy, không phụ thuộc framework Spring.
 */
@Configuration
public class DomainBeansConfig {

    @Bean
    public AuthService authService(UserRepository userRepository,
                                  ArtisanRepository artisanRepository,
                                  RefreshTokenRepository refreshTokenRepository,
                                  PasswordHasher passwordHasher,
                                  TokenIssuer tokenIssuer) {
        return new AuthService(userRepository, artisanRepository, refreshTokenRepository, passwordHasher, tokenIssuer);
    }

    @Bean
    public ArtisanService artisanService(ArtisanRepository artisanRepository) {
        return new ArtisanService(artisanRepository);
    }

    @Bean
    public ProductService productService(ProductRepository productRepository, ArtisanRepository artisanRepository) {
        return new ProductService(productRepository, artisanRepository);
    }

    @Bean
    public CategoryService categoryService(CategoryRepository categoryRepository) {
        return new CategoryService(categoryRepository);
    }

    @Bean
    public BatchService batchService(BatchRepository batchRepository,
                                    ProductionLogRepository logRepository,
                                    ArtisanRepository artisanRepository,
                                    QrCodeGenerator qrCodeGenerator,
                                    @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        return new BatchService(batchRepository, logRepository, artisanRepository, qrCodeGenerator, frontendUrl);
    }

    @Bean
    public TraceService traceService(BatchRepository batchRepository,
                                    ArtisanRepository artisanRepository,
                                    ProductionLogRepository logRepository,
                                    ProductRepository productRepository) {
        return new TraceService(batchRepository, artisanRepository, logRepository, productRepository);
    }

    @Bean
    public QuoteService quoteService(QuoteRepository quoteRepository,
                                    BidRepository bidRepository,
                                    ArtisanRepository artisanRepository) {
        return new QuoteService(quoteRepository, bidRepository, artisanRepository);
    }

    @Bean
    public CartService cartService(CartRepository cartRepository, ProductRepository productRepository) {
        return new CartService(cartRepository, productRepository);
    }

    @Bean
    public OrderService orderService(OrderRepository orderRepository,
                                     CartRepository cartRepository,
                                     ProductRepository productRepository,
                                     ArtisanRepository artisanRepository) {
        return new OrderService(orderRepository, cartRepository, productRepository, artisanRepository);
    }

    @Bean
    public ReviewService reviewService(ReviewRepository reviewRepository) {
        return new ReviewService(reviewRepository);
    }

    @Bean
    public NewsService newsService(NewsRepository newsRepository) {
        return new NewsService(newsRepository);
    }

    @Bean
    public AdminService adminService(ArtisanRepository artisanRepository,
                                     UserRepository userRepository,
                                     QuoteRepository quoteRepository,
                                     ProductRepository productRepository) {
        return new AdminService(artisanRepository, userRepository, quoteRepository, productRepository);
    }
}
