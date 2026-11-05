package com.vcraft.api.news;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.dto.PageResponse;
import com.vcraft.api.news.dto.CreateNewsRequest;
import com.vcraft.api.news.dto.UpdateNewsRequest;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.News;
import com.vcraft.domain.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news")
@Tag(name = "8. Community News", description = "Các API tin tức và thông báo cộng đồng làng nghề (giá đất, lịch cúp điện, chính sách BQL)")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    @Operation(summary = "Danh sách tin tức và thông báo cộng đồng làng nghề")
    public ResponseEntity<PageResponse<News>> getNewsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String type
    ) {
        List<News> data = newsService.getNewsList(type, page, limit);
        long total = newsService.countNews(type);
        return ResponseEntity.ok(PageResponse.of(data, page, limit, total));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một bài tin tức")
    public ResponseEntity<News> getNewsDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(newsService.getNewsDetail(id));
    }

    @PostMapping
    @Operation(summary = "Admin đăng bài tin tức mới")
    public ResponseEntity<News> createNews(Authentication authentication,
                                           @Valid @RequestBody CreateNewsRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        News news = newsService.createNews(
                caller, request.title(), request.content(), request.type(),
                request.publishedAt(), request.images()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(news);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Admin chỉnh sửa bài tin tức")
    public ResponseEntity<News> updateNews(Authentication authentication,
                                           @PathVariable UUID id,
                                           @RequestBody UpdateNewsRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        News updated = newsService.updateNews(caller, id, request.title(), request.content(), request.type());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Admin xóa bài tin tức")
    public ResponseEntity<Map<String, String>> deleteNews(Authentication authentication,
                                                          @PathVariable UUID id) {
        Caller caller = CallerResolver.resolve(authentication);
        newsService.deleteNews(caller, id);
        return ResponseEntity.ok(Map.of("message", "Deleted news successfully"));
    }
}
