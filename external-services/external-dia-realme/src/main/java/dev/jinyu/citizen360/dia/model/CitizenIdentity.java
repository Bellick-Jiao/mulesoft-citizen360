package dev.jinyu.citizen360.dia.model;

public record CitizenIdentity(
        String citizenId,
        String firstName,
        String lastName,
        String dateOfBirth,
        Address address,
        boolean identityVerified,
        String riskBand
) {
    public record Address(String street, String city, String postcode) {}
}
