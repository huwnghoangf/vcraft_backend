package com.vcraft.infrastructure.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.util.UUID;

@Entity
@Table(name = "quote_allocations")
public class QuoteAllocationEntity {
    @Id @Column(name = "child_id") private UUID childId;
    @Column(name = "parent_id", nullable = false) private UUID parentId;
    protected QuoteAllocationEntity() {}
    public QuoteAllocationEntity(UUID parentId, UUID childId) { this.parentId = parentId; this.childId = childId; }
    public UUID getParentId() { return parentId; }
    public UUID getChildId() { return childId; }
}
