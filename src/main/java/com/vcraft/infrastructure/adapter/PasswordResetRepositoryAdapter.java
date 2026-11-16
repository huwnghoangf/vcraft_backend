package com.vcraft.infrastructure.adapter;
import com.vcraft.domain.repository.PasswordResetRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
public class PasswordResetRepositoryAdapter implements PasswordResetRepository {
    private final JdbcTemplate db;
    public PasswordResetRepositoryAdapter(JdbcTemplate db) { this.db = db; }
    public Optional<Token> find(String hash) {
        return db.query("SELECT token_hash,user_id,created_at,expires_at FROM password_reset_tokens WHERE token_hash = ? AND used_at IS NULL",
                (rs, n) -> new Token(rs.getString(1), rs.getObject(2, UUID.class), rs.getTimestamp(3).toInstant(), rs.getTimestamp(4).toInstant()), hash).stream().findFirst();
    }
    public boolean issuedSince(UUID id, Instant since) {
        return db.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE user_id = ? AND created_at > ?", Long.class, id, Timestamp.from(since)) > 0;
    }
    public void invalidateAll(UUID id, Instant now) {
        db.update("UPDATE password_reset_tokens SET used_at = ? WHERE user_id = ? AND used_at IS NULL", Timestamp.from(now), id);
    }
    public void save(Token t) {
        db.update("INSERT INTO password_reset_tokens(token_hash,user_id,created_at,expires_at) VALUES (?,?,?,?)",
                t.hash(), t.userId(), Timestamp.from(t.createdAt()), Timestamp.from(t.expiresAt()));
    }
    public boolean consume(String hash, Instant now) {
        return db.update("UPDATE password_reset_tokens SET used_at = ? WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?",
                Timestamp.from(now), hash, Timestamp.from(now)) == 1;
    }
}
