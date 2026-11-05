package com.vcraft.domain.repository;

import com.vcraft.domain.model.News;
import com.vcraft.domain.model.NewsType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsRepository {
    Optional<News> findById(UUID id);
    News save(News news);
    void deleteById(UUID id);
    List<News> findAll(NewsType type, int page, int limit);
    long count(NewsType type);
}
