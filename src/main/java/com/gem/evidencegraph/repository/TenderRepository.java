package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.entity.TenderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenderRepository extends JpaRepository<Tender, UUID> {

    Optional<Tender> findByTenderReference(String tenderReference);

    List<Tender> findByStatus(TenderStatus status);

}
