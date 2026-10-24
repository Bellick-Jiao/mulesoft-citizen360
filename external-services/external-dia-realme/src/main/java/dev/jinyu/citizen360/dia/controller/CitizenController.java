package dev.jinyu.citizen360.dia.controller;

import dev.jinyu.citizen360.dia.data.IdentityData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/realme/v1/citizens")
public class CitizenController {

    private final IdentityData data;

    public CitizenController(IdentityData data) {
        this.data = data;
    }

    @GetMapping("/{citizenId}")
    public ResponseEntity<?> getCitizen(@PathVariable String citizenId) {
        return data.findById(citizenId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "not_found", "citizenId", citizenId)));
    }
}
