package dev.jinyu.citizen360.mbie.data;

import dev.jinyu.citizen360.mbie.model.License;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class LicenseData {

    // citizenId -> list of licenses
    private static final Map<String, List<License>> DB = Map.of(
            "RN-12345", List.of(
                    new License("LIC-44556", "RN-12345", "DRIVING", "ACTIVE", "2027-03-01"),
                    new License("LIC-44599", "RN-12345", "ALCOHOL", "EXPIRED", "2025-12-15")
            ),
            "RN-22222", List.of(
                    new License("LIC-77881", "RN-22222", "DRIVING", "ACTIVE", "2028-06-20")
            ),
            "RN-33333", List.of()
    );

    public List<License> findByCitizenId(String citizenId) {
        return DB.getOrDefault(citizenId, List.of());
    }
}
