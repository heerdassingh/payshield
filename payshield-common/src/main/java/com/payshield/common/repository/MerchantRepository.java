package com.payshield.common.repository;

import com.payshield.common.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MySQL Repository - Merchant Master Data
 */
@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {

    @Query("SELECT m FROM Merchant m WHERE m.id = :merchantId AND m.status = 'ACTIVE'")
    Optional<Merchant> findActiveMerchantById(String merchantId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Merchant m WHERE m.id = :merchantId AND m.status = 'ACTIVE'")
    boolean isMerchantActiveById(String merchantId);
}
