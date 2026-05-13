package com.hawkstack.api.details.repository;

import com.hawkstack.api.details.entity.UserDetails;
import com.hawkstack.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DetailsRepository extends JpaRepository<UserDetails, Long> {
    Optional<UserDetails> findByUser(User user);
    boolean existsByUser(User user);
}
