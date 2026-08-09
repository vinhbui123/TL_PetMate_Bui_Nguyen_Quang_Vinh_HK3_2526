package com.petmate.server.entity;

import com.petmate.server.enums.ProtectionLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "red_list_species", indexes = {
    @Index(name = "idx_red_list_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedListSpecies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String category;

    @Column(name = "breed_keyword", nullable = false, length = 200)
    private String breedKeyword;

    @Column(columnDefinition = "TEXT")
    private String synonyms;

    @Enumerated(EnumType.STRING)
    @Column(name = "protection_level", nullable = false, length = 20)
    @Builder.Default
    private ProtectionLevel protectionLevel = ProtectionLevel.RESTRICTED;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
