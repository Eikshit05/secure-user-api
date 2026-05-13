package com.hawkstack.api.details.service;

import com.hawkstack.api.details.dto.DetailsRequest;
import com.hawkstack.api.details.dto.DetailsResponse;
import com.hawkstack.api.details.entity.UserDetails;
import com.hawkstack.api.details.repository.DetailsRepository;
import com.hawkstack.api.exception.DetailsNotFoundException;
import com.hawkstack.api.storage.S3Service;
import com.hawkstack.api.user.entity.User;
import com.hawkstack.api.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DetailsService {

    private final DetailsRepository detailsRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public DetailsService(DetailsRepository detailsRepository,
                          UserRepository userRepository,
                          S3Service s3Service) {
        this.detailsRepository = detailsRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    public DetailsResponse create(String email, DetailsRequest request) {
        User user = findUser(email);
        if (detailsRepository.existsByUser(user)) {
            throw new IllegalStateException("Details already exist for this user");
        }
        UserDetails details = new UserDetails();
        details.setUser(user);
        mapRequest(request, details);
        return toResponse(detailsRepository.save(details));
    }

    public DetailsResponse get(String email) {
        User user = findUser(email);
        UserDetails details = detailsRepository.findByUser(user)
                .orElseThrow(() -> new DetailsNotFoundException("No details found for: " + email));
        return toResponse(details);
    }

    public DetailsResponse update(String email, DetailsRequest request) {
        User user = findUser(email);
        UserDetails details = detailsRepository.findByUser(user)
                .orElseThrow(() -> new DetailsNotFoundException("No details found for: " + email));
        mapRequest(request, details);
        return toResponse(detailsRepository.save(details));
    }

    public void delete(String email) {
        User user = findUser(email);
        UserDetails details = detailsRepository.findByUser(user)
                .orElseThrow(() -> new DetailsNotFoundException("No details found for: " + email));
        detailsRepository.delete(details);
    }

    public DetailsResponse uploadPdf(String email, MultipartFile file) {
        User user = findUser(email);
        UserDetails details = detailsRepository.findByUser(user)
                .orElseThrow(() -> new DetailsNotFoundException("No details found for: " + email));
        String url = s3Service.upload(file, "pdf/" + user.getId() + "/" + file.getOriginalFilename());
        details.setPdfUrl(url);
        return toResponse(detailsRepository.save(details));
    }

    public DetailsResponse uploadVideo(String email, MultipartFile file) {
        User user = findUser(email);
        UserDetails details = detailsRepository.findByUser(user)
                .orElseThrow(() -> new DetailsNotFoundException("No details found for: " + email));
        String url = s3Service.upload(file, "video/" + user.getId() + "/" + file.getOriginalFilename());
        details.setVideoUrl(url);
        return toResponse(detailsRepository.save(details));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private void mapRequest(DetailsRequest request, UserDetails details) {
        if (request.getName() != null) details.setName(request.getName());
        if (request.getEmail() != null) details.setEmail(request.getEmail());
        if (request.getCountryCode() != null) details.setCountryCode(request.getCountryCode());
        if (request.getPhone() != null) details.setPhone(request.getPhone());
        if (request.getAddress() != null) details.setAddress(request.getAddress());
    }

    private DetailsResponse toResponse(UserDetails details) {
        DetailsResponse response = new DetailsResponse();
        response.setId(details.getId());
        response.setName(details.getName());
        response.setEmail(details.getEmail());
        response.setCountryCode(details.getCountryCode());
        response.setPhone(details.getPhone());
        response.setAddress(details.getAddress());
        response.setPdfUrl(details.getPdfUrl());
        response.setVideoUrl(details.getVideoUrl());
        return response;
    }
}
