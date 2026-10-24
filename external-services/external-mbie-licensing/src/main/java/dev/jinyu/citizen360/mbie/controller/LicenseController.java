package dev.jinyu.citizen360.mbie.controller;

import dev.jinyu.citizen360.mbie.data.LicenseData;
import dev.jinyu.citizen360.mbie.model.License;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/licensing/v1")
public class LicenseController {

    private final LicenseData data;

    public LicenseController(LicenseData data) {
        this.data = data;
    }

    @GetMapping("/licenses")
    public Map<String, List<License>> getLicenses(@RequestParam String citizenId) {
        return Map.of("licenses", data.findByCitizenId(citizenId));
    }
}
