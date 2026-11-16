package com.vcraft.domain.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.vcraft.domain.exception.*;
import com.vcraft.domain.model.*;
import com.vcraft.domain.repository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
class PasswordResetServiceTest {
    UserRepository users = mock(UserRepository.class);
    PasswordResetRepository tokens = mock(PasswordResetRepository.class);
    RefreshTokenRepository refresh = mock(RefreshTokenRepository.class);
    PasswordHasher hasher = mock(PasswordHasher.class);
    PasswordResetDelivery delivery = mock(PasswordResetDelivery.class);
    Instant now = Instant.parse("2026-11-16T00:00:00Z");
    User user = new User(UUID.randomUUID(), "user@example.com", "old", "User", null, Role.BUYER, UserStatus.ACTIVE, now, now, 4);
    PasswordResetService service = new PasswordResetService(users, tokens, refresh, hasher, delivery, Clock.fixed(now, ZoneOffset.UTC));
    @BeforeEach void setup() {
        when(delivery.available()).thenReturn(true);
        when(users.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(users.findByIdForUpdate(user.id())).thenReturn(Optional.of(user));
    }
    @Test void onlyHashedExpiringTokenIsStored() {
        service.request(" USER@example.com ");
        var raw = ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq(user.email()), raw.capture());
        var saved = ArgumentCaptor.forClass(PasswordResetRepository.Token.class);
        verify(tokens).save(saved.capture());
        assertThat(raw.getValue()).hasSize(43);
        assertThat(saved.getValue().hash()).isEqualTo(PasswordResetService.hash(raw.getValue())).doesNotContain(raw.getValue());
        assertThat(saved.getValue().expiresAt()).isEqualTo(now.plusSeconds(900));
    }
    @Test void cooldownAndUnknownAccountDoNotSendEmail() {
        when(tokens.issuedSince(eq(user.id()), any())).thenReturn(true);
        service.request(user.email()); service.request("unknown@example.com");
        verify(delivery, never()).send(any(), any()); verify(tokens, never()).save(any());
    }
    @Test void disabledDeliveryFailsBeforeLookingUpAccount() {
        when(delivery.available()).thenReturn(false);
        assertThatThrownBy(() -> service.request(user.email())).isInstanceOf(ServiceUnavailableException.class);
        verifyNoInteractions(users);
    }
    @Test void resetIncrementsSecurityVersionAndRevokesRefreshTokens() {
        String raw = "a".repeat(43), hash = PasswordResetService.hash(raw);
        when(tokens.find(hash)).thenReturn(Optional.of(new PasswordResetRepository.Token(hash, user.id(), now, now.plusSeconds(900))));
        when(tokens.consume(hash, now)).thenReturn(true); when(hasher.hash("new-password")).thenReturn("new-hash");
        service.reset(raw, "new-password");
        var changed = ArgumentCaptor.forClass(User.class); verify(users).save(changed.capture());
        assertThat(changed.getValue().securityVersion()).isEqualTo(5);
        assertThat(changed.getValue().passwordHash()).isEqualTo("new-hash");
        verify(refresh).deleteByUserId(user.id());
    }
    @Test void expiredOrAlreadyConsumedTokenCannotChangePassword() {
        String raw = "a".repeat(43), hash = PasswordResetService.hash(raw);
        when(tokens.find(hash)).thenReturn(Optional.of(new PasswordResetRepository.Token(hash, user.id(), now.minusSeconds(900), now)));
        assertThatThrownBy(() -> service.reset(raw, "new-password")).isInstanceOf(InvalidInputException.class);
        when(tokens.find(hash)).thenReturn(Optional.of(new PasswordResetRepository.Token(hash, user.id(), now, now.plusSeconds(900))));
        when(tokens.consume(hash, now)).thenReturn(false);
        assertThatThrownBy(() -> service.reset(raw, "new-password")).isInstanceOf(InvalidInputException.class);
        verify(users, never()).save(any());
    }
}
