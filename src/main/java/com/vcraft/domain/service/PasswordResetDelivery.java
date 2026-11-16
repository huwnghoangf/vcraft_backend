package com.vcraft.domain.service;
public interface PasswordResetDelivery {
    boolean available();
    void send(String recipient, String token);
}
