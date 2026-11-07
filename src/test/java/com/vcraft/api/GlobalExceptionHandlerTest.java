package com.vcraft.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vcraft.domain.exception.ConflictException;
import com.vcraft.domain.exception.ForbiddenException;
import com.vcraft.domain.exception.InvalidInputException;
import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("Không tìm thấy sản phẩm gốm");
        }

        @GetMapping("/unauthorized")
        void unauthorized() {
            throw new UnauthorizedException("Vui lòng đăng nhập để tiếp tục");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException("Chỉ Quản trị viên BQL mới có quyền thực hiện");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("Email đã được đăng ký trong hệ thống");
        }

        @GetMapping("/invalid-input")
        void invalidInput() {
            throw new InvalidInputException("Số lượng đặt hàng không thể âm");
        }

        @GetMapping("/duplicate-db")
        void duplicateDb() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint users_email_key");
        }

        record DummyBody(String name) {}

        @PostMapping(value = "/json-body", consumes = MediaType.APPLICATION_JSON_VALUE)
        void jsonBody(@RequestBody DummyBody body) {
        }

        @GetMapping("/server-error")
        void serverError() {
            throw new RuntimeException("Lỗi hệ thống không mong đợi");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("NotFoundException chuyển đổi thành HTTP 404 Not Found")
    void notFoundExceptionBecomes404() throws Exception {
        mockMvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm gốm"))
                .andExpect(jsonPath("$.path").value("/not-found"));
    }

    @Test
    @DisplayName("UnauthorizedException chuyển đổi thành HTTP 401 Unauthorized")
    void unauthorizedExceptionBecomes401() throws Exception {
        mockMvc.perform(get("/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập để tiếp tục"));
    }

    @Test
    @DisplayName("ForbiddenException chuyển đổi thành HTTP 403 Forbidden")
    void forbiddenExceptionBecomes403() throws Exception {
        mockMvc.perform(get("/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Chỉ Quản trị viên BQL mới có quyền thực hiện"));
    }

    @Test
    @DisplayName("ConflictException chuyển đổi thành HTTP 409 Conflict")
    void conflictExceptionBecomes409() throws Exception {
        mockMvc.perform(get("/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email đã được đăng ký trong hệ thống"));
    }

    @Test
    @DisplayName("InvalidInputException chuyển đổi thành HTTP 400 Bad Request")
    void invalidInputExceptionBecomes400() throws Exception {
        mockMvc.perform(get("/invalid-input"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Số lượng đặt hàng không thể âm"));
    }

    @Test
    @DisplayName("DataIntegrityViolationException chuyển đổi thành HTTP 409 Conflict")
    void dataIntegrityViolationBecomes409() throws Exception {
        mockMvc.perform(get("/duplicate-db"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc dữ liệu trong hệ thống"));
    }

    @Test
    @DisplayName("Dữ liệu JSON gửi lên sai định dạng chuyển đổi thành HTTP 400 Bad Request")
    void malformedJsonBecomes400() throws Exception {
        mockMvc.perform(post("/json-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Lỗi runtime bất ngờ chuyển đổi thành HTTP 500 Internal Server Error")
    void unexpectedExceptionBecomes500() throws Exception {
        mockMvc.perform(get("/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Đã xảy ra lỗi nội bộ máy chủ, vui lòng thử lại sau"));
    }
}
