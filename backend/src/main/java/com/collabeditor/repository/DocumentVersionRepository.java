package com.collabeditor.repository;

import com.collabeditor.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findByDocumentIdOrderByVersionDesc(Long documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersion(Long documentId, Integer version);

    @Query("SELECT COALESCE(MAX(v.version), 0) FROM DocumentVersion v WHERE v.document.id = :documentId")
    Integer findMaxVersionByDocumentId(@Param("documentId") Long documentId);

    long countByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}
