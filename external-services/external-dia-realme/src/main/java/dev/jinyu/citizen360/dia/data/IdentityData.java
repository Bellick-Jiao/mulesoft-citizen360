package dev.jinyu.citizen360.dia.data;

import dev.jinyu.citizen360.dia.model.CitizenIdentity;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class IdentityData {

    // In-memory mock of RealMe identity records.
    private static final Map<String, CitizenIdentity> DB = Map.of(
            "RN-12345", new CitizenIdentity(
                    "RN-12345", "Jinyu", "Liang", "1983-05-12",
                    new CitizenIdentity.Address("330 Cashel Street", "Christchurch", "8011"),
                    true, "LOW"),
            "RN-22222", new CitizenIdentity(
                    "RN-22222", "Maria", "Garcia", "1990-08-22",
                    new CitizenIdentity.Address("15 Cuba Street", "Wellington", "6011"),
                    true, "MEDIUM"),
            "RN-33333", new CitizenIdentity(
                    "RN-33333", "Hiro", "Tanaka", "1975-11-03",
                    new CitizenIdentity.Address("88 Queen Street", "Auckland", "1010"),
                    false, "HIGH")
    );

    public Optional<CitizenIdentity> findById(String citizenId) {
        return Optional.ofNullable(DB.get(citizenId));
    }
}
