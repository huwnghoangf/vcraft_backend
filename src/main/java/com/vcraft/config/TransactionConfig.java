package com.vcraft.config;

import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/** Atomic use cases without introducing Spring dependencies into the domain. */
@Configuration
public class TransactionConfig {
    @Bean
    public TransactionInterceptor domainTransactionInterceptor(PlatformTransactionManager manager) {
        var source = new MatchAlwaysTransactionAttributeSource();
        source.setTransactionAttribute(new RuleBasedTransactionAttribute());
        return new TransactionInterceptor(manager, source);
    }

    @Bean
    public static BeanNameAutoProxyCreator domainTransactions() {
        var proxy = new BeanNameAutoProxyCreator();
        proxy.setBeanNames("authService", "artisanService", "productService", "categoryService",
                "batchService", "traceService", "quoteService", "cartService", "orderService",
                "reviewService", "newsService", "adminService");
        proxy.setInterceptorNames("domainTransactionInterceptor");
        proxy.setProxyTargetClass(true);
        return proxy;
    }
}
