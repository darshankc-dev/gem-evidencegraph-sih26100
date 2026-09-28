package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.Bidder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidderRepository extends JpaRepository<Bidder, UUID> {

    Optional<Bidder> findByPan(String pan);

    Optional<Bidder> findByGstin(String gstin);

    Optional<Bidder> findByUdyamNumber(String udyamNumber);

    List<Bidder> findByNormalizedName(String normalizedName);

}
