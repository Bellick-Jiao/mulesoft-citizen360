package dev.jinyu.citizen360.casemgmt.data;

import dev.jinyu.citizen360.casemgmt.model.CaseRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class CaseData {

    // citizenId -> list of cases. Mirrors the legacy CMS.
    // lastModifiedDate is used by batch incremental sync.
    private static final Map<String, List<CaseRecord>> DB = Map.of(
            "RN-12345", List.of(
                    new CaseRecord("CASE-2026-001", "RN-12345", "BENEFIT",
                            "IN_PROGRESS", "2026-09-15", "J. Smith",
                            "2026-10-20T08:30:00Z"),
                    new CaseRecord("CASE-2026-014", "RN-12345", "HOUSING",
                            "DECIDED", "2026-03-02", "M. Chen",
                            "2026-06-10T14:00:00Z")
            ),
            "RN-22222", List.of(
                    new CaseRecord("CASE-2026-007", "RN-22222", "DISABILITY",
                            "NEW", "2026-10-18", null,
                            "2026-10-18T09:15:00Z")
            ),
            "RN-33333", List.of()
    );

    public List<CaseRecord> findByCitizenId(String citizenId) {
        return DB.getOrDefault(citizenId, List.of());
    }
}
