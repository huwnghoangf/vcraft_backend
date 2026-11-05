package com.vcraft.domain.service;

import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.News;
import com.vcraft.domain.model.NewsType;
import com.vcraft.domain.repository.NewsRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class NewsService {

    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public List<News> getNewsList(String type, int page, int limit) {
        NewsType newsType = type != null ? NewsType.fromString(type) : null;
        return newsRepository.findAll(newsType, page, limit);
    }

    public long countNews(String type) {
        NewsType newsType = type != null ? NewsType.fromString(type) : null;
        return newsRepository.count(newsType);
    }

    public News getNewsDetail(UUID id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài tin tức"));
    }

    public News createNews(Caller caller, String title, String content, String type,
                           Instant publishedAt, List<String> images) {
        caller.requireAdmin();
        News news = new News(
                UUID.randomUUID(),
                caller.requireUserId(),
                title,
                content,
                NewsType.fromString(type),
                images != null ? images : List.of(),
                publishedAt != null ? publishedAt : Instant.now(),
                Instant.now(),
                Instant.now()
        );
        return newsRepository.save(news);
    }

    public News updateNews(Caller caller, UUID id, String title, String content, String type) {
        caller.requireAdmin();
        News current = getNewsDetail(id);

        News updated = new News(
                current.id(),
                current.authorId(),
                title != null ? title : current.title(),
                content != null ? content : current.content(),
                type != null ? NewsType.fromString(type) : current.type(),
                current.images(),
                current.publishedAt(),
                current.createdAt(),
                Instant.now()
        );
        return newsRepository.save(updated);
    }

    public void deleteNews(Caller caller, UUID id) {
        caller.requireAdmin();
        newsRepository.deleteById(id);
    }
}
