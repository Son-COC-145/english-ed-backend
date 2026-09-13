package com.example.english_app.entity.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stored_files")
@Getter
@NoArgsConstructor
public class StoredFileRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20) private String provider;
    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT") private String objectKey;
    @Column(name = "resource_type", nullable = false, length = 20) private String resourceType;
    @Column(name = "content_hash", nullable = false, length = 64) private String contentHash;
    @Column(columnDefinition = "TEXT") private String url;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "attempt_count", nullable = false) private Integer attempts;
    @Column(name = "claim_token", length = 36) private String claimToken;
}
