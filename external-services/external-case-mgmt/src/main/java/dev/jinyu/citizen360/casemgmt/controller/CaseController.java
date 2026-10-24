package dev.jinyu.citizen360.casemgmt.controller;

import dev.jinyu.citizen360.casemgmt.data.CaseData;
import dev.jinyu.citizen360.casemgmt.model.CaseRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cases/v1")
public class CaseController {

    private final CaseData data;

    public CaseController(CaseData data) {
        this.data = data;
    }

    @GetMapping
    public Map<String, List<CaseRecord>> getCases(@RequestParam String citizenId) {
        return Map.of("cases", data.findByCitizenId(citizenId));
    }
}
