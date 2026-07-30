package com.collabeditor.repository;

import com.collabeditor.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwnerId(Long ownerId);

    @Query("SELECT d FROM Document d WHERE d.owner.id = :userId OR d.id IN " +
            "(SELECT dp.document.id FROM DocumentPermission dp WHERE dp.user.id = :userId)")
    List<Document> findAccessibleByUserId(@Param("userId") Long userId);
}
