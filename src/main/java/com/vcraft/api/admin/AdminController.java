package com.vcraft.api.admin;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.dto.PageResponse;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.QuoteRequest;
import com.vcraft.domain.model.User;
import com.vcraft.domain.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "9. Admin Management", description = "Các API dành cho Ban Quản Lý làng nghề gốm Bát Tràng (Duyệt nghệ nhân, quản lý tài khoản, báo cáo)")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/artisans/pending")
    @Operation(summary = "Danh sách nghệ nhân đang chờ duyệt hồ sơ đăng ký vào làng nghề")
    public ResponseEntity<PageResponse<ArtisanProfile>> getPendingArtisans(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Caller caller = CallerResolver.resolve(authentication);
        List<ArtisanProfile> data = adminService.getPendingArtisans(caller, page, limit);
        long total = adminService.countPendingArtisans(caller);
        return ResponseEntity.ok(PageResponse.of(data, page, limit, total));
    }

    @PatchMapping("/artisans/{id}/approve")
    @Operation(summary = "BQL duyệt và kích hoạt tài khoản chính thức cho nghệ nhân xưởng gốm")
    public ResponseEntity<ArtisanProfile> approveArtisan(Authentication authentication,
                                                         @PathVariable UUID id) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(adminService.approveArtisan(caller, id));
    }

    @PatchMapping("/artisans/{id}/reject")
    @Operation(summary = "Từ chối hồ sơ đăng ký nghệ nhân")
    public ResponseEntity<ArtisanProfile> rejectArtisan(Authentication authentication,
                                                        @PathVariable UUID id,
                                                        @RequestBody(required = false) Map<String, String> body) {
        Caller caller = CallerResolver.resolve(authentication);
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(adminService.rejectArtisan(caller, id, reason));
    }

    @PatchMapping("/artisans/{id}/block")
    @Operation(summary = "Khóa tài khoản xưởng gốm vi phạm quy định làng nghề")
    public ResponseEntity<ArtisanProfile> blockArtisan(Authentication authentication,
                                                       @PathVariable UUID id,
                                                       @RequestBody(required = false) Map<String, String> body) {
        Caller caller = CallerResolver.resolve(authentication);
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(adminService.blockArtisan(caller, id, reason));
    }

    @GetMapping("/users")
    @Operation(summary = "Danh sách tất cả người dùng trong hệ thống")
    public ResponseEntity<PageResponse<User>> getUsers(
            Authentication authentication,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search
    ) {
        Caller caller = CallerResolver.resolve(authentication);
        List<User> data = adminService.getUsers(caller, role, page, limit, search);
        long total = adminService.countUsers(caller, role, search);
        return ResponseEntity.ok(PageResponse.of(data, page, limit, total));
    }

    @PatchMapping("/users/{id}/block")
    @Operation(summary = "Khóa tài khoản người dùng")
    public ResponseEntity<User> blockUser(Authentication authentication,
                                          @PathVariable UUID id,
                                          @RequestBody(required = false) Map<String, String> body) {
        Caller caller = CallerResolver.resolve(authentication);
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(adminService.blockUser(caller, id, reason));
    }

    @GetMapping("/quotes")
    @Operation(summary = "BQL xem toàn bộ yêu cầu báo giá sỉ trong hệ thống")
    public ResponseEntity<PageResponse<QuoteRequest>> getAllQuotes(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Caller caller = CallerResolver.resolve(authentication);
        List<QuoteRequest> data = adminService.getAllQuotes(caller, status, page, limit);
        long total = adminService.countAllQuotes(caller, status);
        return ResponseEntity.ok(PageResponse.of(data, page, limit, total));
    }

    @GetMapping("/stats")
    @Operation(summary = "Thống kê tổng quan hệ thống làng nghề (Dashboard)")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(adminService.getDashboardStats(caller));
    }
}
