package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.OrderItemEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, UUID> {}
