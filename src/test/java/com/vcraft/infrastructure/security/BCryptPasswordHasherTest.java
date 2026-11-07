package com.vcraft.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    @DisplayName("Mật khẩu sau khi hash không trùng chuỗi gốc và match thành công")
    void hashIsNotThePlainPasswordAndMatches() {
        String hash = hasher.hash("uet123456");

        assertThat(hash).isNotEqualTo("uet123456").startsWith("$2a$10$");
        assertThat(hasher.matches("uet123456", hash)).isTrue();
    }

    @Test
    @DisplayName("Kiểm tra mật khẩu sai phải trả về false")
    void wrongPasswordDoesNotMatch() {
        String hash = hasher.hash("uet123456");

        assertThat(hasher.matches("wrongpassword", hash)).isFalse();
        assertThat(hasher.matches("UET123456", hash)).isFalse();
    }

    @Test
    @DisplayName("Cùng một mật khẩu nhưng 2 lần hash sinh ra chuỗi khác nhau do salt ngẫu nhiên")
    void samePasswordGivesDifferentHashes() {
        assertThat(hasher.hash("secret123")).isNotEqualTo(hasher.hash("secret123"));
    }

    @Test
    @DisplayName("Hash mật khẩu seed mẫu trong V2__seed_initial_data.sql phải match với uet123456")
    void seededSampleUserHashMatchesDocumentedPassword() {
        String seededHash = "$2a$10$jqmc./tQysLyXTNGg8M4CeIlAO5GffLskOQWPRLjPYfLidU3fVWEO";

        assertThat(hasher.matches("uet123456", seededHash)).isTrue();
    }
}
