package com.vcraft.api.map;

import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.service.ArtisanService;
import com.vcraft.domain.service.ProductService;
import com.vcraft.domain.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/map")
@Tag(name = "6. Village Map", description = "Các API bản đồ số tương tác làng nghề gốm Bát Tràng (Ghim xưởng & Popup thông tin)")
public class MapController {

    public record MapPinResponse(
            UUID id,
            String name,
            Double lat,
            Double lng,
            boolean openStatus,
            String thumbnail,
            String phone
    ) {}

    private final ArtisanService artisanService;
    private final ReviewService reviewService;
    private final ProductService productService;

    public MapController(ArtisanService artisanService, ReviewService reviewService, ProductService productService) {
        this.artisanService = artisanService;
        this.reviewService = reviewService;
        this.productService = productService;
    }

    @GetMapping("/pins")
    @Operation(summary = "Lấy danh sách ghim xưởng gốm để hiển thị trên bản đồ số")
    public ResponseEntity<List<MapPinResponse>> getMapPins(@RequestParam(defaultValue = "false") boolean openOnly) {
        List<MapPinResponse> pins = artisanService.getMapPins(openOnly).stream()
                .map(a -> new MapPinResponse(
                        a.id(),
                        a.workshopName(),
                        a.latitude(),
                        a.longitude(),
                        a.openStatus(),
                        a.images() != null && !a.images().isEmpty() ? a.images().get(0) : null,
                        a.phone()
                ))
                .toList();
        return ResponseEntity.ok(pins);
    }

    @GetMapping("/pins/{id}")
    @Operation(summary = "Thông tin popup khi người dùng click vào ghim trên bản đồ")
    public ResponseEntity<Map<String, Object>> getPinPopup(@PathVariable UUID id) {
        ArtisanProfile artisan = artisanService.getArtisanDetail(id);
        Double avgRating = reviewService.getArtisanAvgRating(id);
        long productCount = productService.countProducts(id, null, null, null);

        return ResponseEntity.ok(Map.of(
                "id", artisan.id(),
                "name", artisan.workshopName(),
                "ownerName", artisan.ownerName(),
                "address", artisan.address(),
                "openStatus", artisan.openStatus(),
                "phone", artisan.phone() != null ? artisan.phone() : "",
                "rating", avgRating != null ? avgRating : 5.0,
                "productCount", productCount,
                "description", artisan.description() != null ? artisan.description() : ""
        ));
    }
}
