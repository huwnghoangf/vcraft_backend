package com.vcraft.api;

import com.vcraft.domain.exception.UnauthorizedException;
import com.vcraft.domain.model.Caller;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

public final class CallerResolver {

    private CallerResolver() {}

    public static Caller resolve(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện");
        }
        if (authentication.getPrincipal() instanceof Caller caller) {
            return caller;
        }
        throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện");
    }

    public static Caller resolveOrAnonymous(Authentication authentication) {
        return isAuthenticated(authentication) ? resolve(authentication) : Caller.anonymous();
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
