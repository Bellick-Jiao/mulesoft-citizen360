package dev.jinyu.citizen360.casemgmt.model;

public record CaseRecord(
        String caseId,
        String citizenId,
        String type,
        String status,
        String openedDate,
        String officer,
        String lastModifiedDate
) {}
