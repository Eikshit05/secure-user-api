package com.hawkstack.api.details.controller;

import com.hawkstack.api.details.dto.DetailsRequest;
import com.hawkstack.api.details.dto.DetailsResponse;
import com.hawkstack.api.details.service.DetailsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/details")
public class DetailsController {

    private final DetailsService detailsService;

    public DetailsController(DetailsService detailsService) {
        this.detailsService = detailsService;
    }

    @PostMapping
    public ResponseEntity<DetailsResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody DetailsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(detailsService.create(principal.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<DetailsResponse> get(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(detailsService.get(principal.getUsername()));
    }

    @PutMapping
    public ResponseEntity<DetailsResponse> update(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody DetailsRequest request) {
        return ResponseEntity.ok(detailsService.update(principal.getUsername(), request));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails principal) {
        detailsService.delete(principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload/pdf")
    public ResponseEntity<DetailsResponse> uploadPdf(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(detailsService.uploadPdf(principal.getUsername(), file));
    }

    @PostMapping("/upload/video")
    public ResponseEntity<DetailsResponse> uploadVideo(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(detailsService.uploadVideo(principal.getUsername(), file));
    }
}
