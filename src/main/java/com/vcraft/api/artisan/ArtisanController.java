package com.vcraft.api.artisan;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.artisan.dto.LocationRequest;
import com.vcraft.api.artisan.dto.OpenStatusRequest;
import com.vcraft.api.artisan.dto.UpdateArtisanRequest;
import com.vcraft.api.dto.PageResponse;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.service.ArtisanService;
import com.vcraft.domain.service.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/artisans")
@Tag(name = "2. Artisan Profile", description = "Các API quản lý hồ sơ xưởng, nghệ nhân, hình ảnh xưởng và trạng thái mở cửa")
public class ArtisanController {

    private final ArtisanService artisanService;
    private final ImageStorageService imageStorageService;

    public ArtisanController(ArtisanService artisanService, ImageStorageService imageStorageService) {
        this.artisanService = artisanService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    @Operation(summary = "Danh sách tất cả xưởng gốm đã được BQL duyệt")
    public ResponseEntity<PageResponse<ArtisanProfile>> getArtisans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String district
    ) {
        List<ArtisanProfile> data = artisanService.getApprovedArtisans(page, limit, search, district);
        long total = artisanService.countApproved(search, district);
        return ResponseEntity.ok(PageResponse.of(data, page, limit, total));
    }

    @GetMapping("/map")
    @Operation(summary = "Lấy tọa độ tất cả xưởng để hiển thị bản đồ số làng nghề")
    public ResponseEntity<List<ArtisanProfile>> getMapArtisans(
            @RequestParam(defaultValue = "false") boolean openOnly
    ) {
        return ResponseEntity.ok(artisanService.getMapPins(openOnly));
    }

    @GetMapping("/me")
    @Operation(summary = "Xem hồ sơ xưởng của nghệ nhân đang đăng nhập")
    public ResponseEntity<ArtisanProfile> getMyProfile(Authentication authentication) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(artisanService.getMyProfile(caller));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật hồ sơ xưởng của nghệ nhân đang đăng nhập")
    public ResponseEntity<ArtisanProfile> updateMyProfile(Authentication authentication,
                                                         @RequestBody UpdateArtisanRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        ArtisanProfile updated = artisanService.updateMyProfile(
                caller, request.workshopName(), request.description(), request.address(),
                request.lat(), request.lng(), request.openStatus(), request.images(),
                request.familyHistory(), request.certificates()
        );
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/me/open-status")
    @Operation(summary = "Bật / tắt trạng thái mở cửa đón khách tham quan & trải nghiệm vuốt gốm")
    public ResponseEntity<ArtisanProfile> toggleOpenStatus(Authentication authentication,
                                                          @RequestBody OpenStatusRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(artisanService.setOpenStatus(caller, request.openStatus()));
    }

    @PutMapping("/me/location")
    @Operation(summary = "Nghệ nhân cập nhật tọa độ vị trí xưởng trên bản đồ")
    public ResponseEntity<ArtisanProfile> updateLocation(Authentication authentication,
                                                        @RequestBody LocationRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(artisanService.updateLocation(caller, request.lat(), request.lng(), request.address()));
    }

    @PostMapping(value = "/me/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ảnh xưởng sản xuất (tự động tối ưu hóa kích cỡ)")
    public ResponseEntity<List<String>> uploadImages(Authentication authentication,
                                                     @RequestParam("files") List<MultipartFile> files) {
        Caller caller = CallerResolver.resolve(authentication);
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String url = imageStorageService.storeImage(file.getInputStream(), file.getOriginalFilename(), "artisans");
                imageUrls.add(url);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi lưu ảnh: " + e.getMessage());
            }
        }
        artisanService.addWorkshopImages(caller, imageUrls);
        return ResponseEntity.ok(imageUrls);
    }

    @DeleteMapping("/me/images/{imageId}")
    @Operation(summary = "Xóa một ảnh khỏi hồ sơ xưởng")
    public ResponseEntity<Map<String, String>> deleteImage(Authentication authentication,
                                                           @PathVariable String imageId) {
        Caller caller = CallerResolver.resolve(authentication);
        artisanService.removeWorkshopImage(caller, imageId);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một xưởng gốm (hồ sơ công khai)")
    public ResponseEntity<ArtisanProfile> getArtisanDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(artisanService.getArtisanDetail(id));
    }
}
