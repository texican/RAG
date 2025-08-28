package com.enterprise.rag.shared.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base entity class that provides common fields and functionality for all entities
 * in the Enterprise RAG system.
 * 
 * <p>This abstract class implements the standard audit fields pattern and provides
 * automatic UUID generation for primary keys. All entities should extend this class
 * to ensure consistent data model structure and automatic audit trail tracking.</p>
 * 
 * <p>Features provided:</p>
 * <ul>
 *   <li>UUID-based primary key generation</li>
 *   <li>Automatic creation and modification timestamp tracking</li>
 *   <li>Optimistic locking support via version field</li>
 *   <li>Standard equals() and hashCode() implementation</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * @Entity
 * @Table(name = "users")
 * public class User extends BaseEntity {
 *     @Column(name = "email", nullable = false, unique = true)
 *     private String email;
 *     
 *     // Additional fields and methods...
 * }
 * }</pre>
 * 
 * @author Enterprise RAG Development Team
 * @since 1.0.0
 * @version 1.0
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * The unique identifier for this entity.
     * Generated automatically using UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Timestamp when this entity was created.
     * Automatically set by JPA auditing on first persist.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this entity was last modified.
     * Automatically updated by JPA auditing on each save operation.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Version field for optimistic locking.
     * Automatically incremented on each update to prevent concurrent modification issues.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Gets the unique identifier of this entity.
     *
     * @return the UUID identifier, or null if the entity has not been persisted yet
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this entity.
     * <p>Note: This should generally not be called directly as the ID is auto-generated.</p>
     *
     * @param id the UUID identifier to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the timestamp when this entity was created.
     *
     * @return the creation timestamp, automatically set by JPA auditing
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     * <p>Note: This is automatically managed by JPA auditing and should not be called directly.</p>
     *
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the timestamp when this entity was last modified.
     *
     * @return the last modification timestamp, automatically updated by JPA auditing
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last modification timestamp.
     * <p>Note: This is automatically managed by JPA auditing and should not be called directly.</p>
     *
     * @param updatedAt the last modification timestamp to set
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Gets the version number for optimistic locking.
     *
     * @return the version number, automatically incremented on each update
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version number for optimistic locking.
     * <p>Note: This is automatically managed by JPA and should not be called directly.</p>
     *
     * @param version the version number to set
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Compares this entity with another object for equality.
     * <p>Two entities are considered equal if they are of the same type and have the same ID.
     * This follows the entity equality pattern where identity is based on the database identifier.</p>
     *
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    /**
     * Returns a hash code for this entity.
     * <p>The hash code is based on the class type to ensure consistent behavior
     * across different entity states (transient vs. persistent).</p>
     *
     * @return the hash code value for this entity
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}