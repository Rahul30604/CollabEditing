package com.collabeditor.repository;

import com.collabeditor.entity.DocumentPermission;
import com.collabeditor.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {

    Optional<DocumentPermission> findByDocumentIdAndUserId(Long documentId, Long userId);

    List<DocumentPermission> findByDocumentId(Long documentId);

    List<DocumentPermission> findByUserId(Long userId);

    boolean existsByDocumentIdAndUserIdAndRole(Long documentId, Long userId, Role role);

    void deleteByDocumentId(Long documentId);
}
