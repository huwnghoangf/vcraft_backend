package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.News;
import com.vcraft.domain.model.NewsType;
import com.vcraft.domain.repository.NewsRepository;
import com.vcraft.infrastructure.entity.NewsEntity;
import com.vcraft.infrastructure.repository.NewsJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class NewsRepositoryAdapter implements NewsRepository {

    private final NewsJpaRepository jpaRepository;

    public NewsRepositoryAdapter(NewsJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<News> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public News save(News news) {
        NewsEntity entity = toEntity(news);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<News> findAll(NewsType type, int page, int limit) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit));
        String typeStr = type != null ? type.name().toLowerCase() : null;
        return jpaRepository.findByType(typeStr, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count(NewsType type) {
        String typeStr = type != null ? type.name().toLowerCase() : null;
        return jpaRepository.countByType(typeStr);
    }

    private News toDomain(NewsEntity entity) {
        return new News(
                entity.getId(),
                entity.getAuthorId(),
                entity.getTitle(),
                entity.getContent(),
                NewsType.fromString(entity.getType()),
                entity.getImages(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private NewsEntity toEntity(News domain) {
        return new NewsEntity(
                domain.id(),
                domain.authorId(),
                domain.title(),
                domain.content(),
                domain.type().name().toLowerCase(),
                domain.images(),
                domain.publishedAt(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
