package dev.jinyu.citizen360.mbie.model;

public record License(
        String licenseId,
        String citizenId,
        String type,
        String status,
        String expiryDate
) {}
