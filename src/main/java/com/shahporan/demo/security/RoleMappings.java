package com.shahporan.demo.security;

/**
 * Central mapping between DB roleInt values and Spring Security authorities.
 */
public final class RoleMappings {

    public static final int BUYER = 0;
    public static final int SELLER = 1;
    public static final int ADMIN = 2;

    private RoleMappings() {
    }

    public static String toAuthority(Integer roleInt) {
        if (roleInt == null) {
            return "ROLE_BUYER";
        }

        return switch (roleInt) {
            case SELLER -> "ROLE_SELLER";
            case ADMIN -> "ROLE_ADMIN";
            case BUYER -> "ROLE_BUYER";
            default -> "ROLE_BUYER";
        };
    }
}

