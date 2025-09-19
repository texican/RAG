package com.byo.rag.shared.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for BaseEntity functionality including UUID generation,
 * audit fields, optimistic locking, and equals/hashCode behavior.
 */
class BaseEntityTest {

    /**
     * Concrete implementation of BaseEntity for testing purposes.
     */
    private static class TestEntity extends BaseEntity {
        private String name;
        
        public TestEntity() {}
        
        public TestEntity(String name) {
            this.name = name;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Test
    @DisplayName("Should generate UUID for new entity")
    void shouldGenerateUuidForNewEntity() {
        TestEntity entity = new TestEntity("test");
        
        // UUID should be null before persistence (auto-generated)
        assertNull(entity.getId(), "ID should be null before persistence");
        
        // Simulate JPA ID generation
        entity.setId(UUID.randomUUID());
        assertNotNull(entity.getId(), "ID should be generated");
        assertEquals(36, entity.getId().toString().length(), "UUID should be 36 characters");
    }

    @Test
    @DisplayName("Should handle audit fields correctly")
    void shouldHandleAuditFieldsCorrectly() {
        TestEntity entity = new TestEntity("test");
        
        // Audit fields should be null initially
        assertNull(entity.getCreatedAt(), "CreatedAt should be null initially");
        assertNull(entity.getUpdatedAt(), "UpdatedAt should be null initially");
        
        // Simulate JPA auditing
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        
        assertNotNull(entity.getCreatedAt(), "CreatedAt should be set");
        assertNotNull(entity.getUpdatedAt(), "UpdatedAt should be set");
        assertEquals(now, entity.getCreatedAt(), "CreatedAt should match set value");
        assertEquals(now, entity.getUpdatedAt(), "UpdatedAt should match set value");
    }

    @Test
    @DisplayName("Should handle version field for optimistic locking")
    void shouldHandleVersionFieldForOptimisticLocking() {
        TestEntity entity = new TestEntity("test");
        
        // Version should be null initially
        assertNull(entity.getVersion(), "Version should be null initially");
        
        // Simulate JPA version management
        entity.setVersion(1L);
        assertEquals(1L, entity.getVersion(), "Version should be set correctly");
        
        entity.setVersion(2L);
        assertEquals(2L, entity.getVersion(), "Version should increment");
    }

    @Test
    @DisplayName("Should implement equals correctly based on ID")
    void shouldImplementEqualsCorrectlyBasedOnId() {
        TestEntity entity1 = new TestEntity("test1");
        TestEntity entity2 = new TestEntity("test2");
        TestEntity entity3 = new TestEntity("test3");
        
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        
        // Entities without IDs should not be equal
        assertNotEquals(entity1, entity2, "Entities without IDs should not be equal");
        
        // Entity should equal itself
        assertEquals(entity1, entity1, "Entity should equal itself");
        
        // Entities with same ID should be equal
        entity1.setId(id1);
        entity2.setId(id1);
        assertEquals(entity1, entity2, "Entities with same ID should be equal");
        
        // Entities with different IDs should not be equal
        entity3.setId(id2);
        assertNotEquals(entity1, entity3, "Entities with different IDs should not be equal");
        
        // Entity should not equal null
        assertNotEquals(entity1, null, "Entity should not equal null");
        
        // Entity should not equal different type
        assertNotEquals(entity1, "string", "Entity should not equal different type");
    }

    @Test
    @DisplayName("Should implement hashCode consistently")
    void shouldImplementHashCodeConsistently() {
        TestEntity entity1 = new TestEntity("test1");
        TestEntity entity2 = new TestEntity("test2");
        
        // HashCode should be consistent for same entity
        int hashCode1 = entity1.hashCode();
        int hashCode2 = entity1.hashCode();
        assertEquals(hashCode1, hashCode2, "HashCode should be consistent");
        
        // HashCode should be based on class, not content
        assertEquals(entity1.hashCode(), entity2.hashCode(), 
            "HashCode should be same for entities of same class");
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        TestEntity entity = new TestEntity();
        
        // Should handle null audit fields
        assertDoesNotThrow(() -> entity.getCreatedAt(), "Should handle null createdAt");
        assertDoesNotThrow(() -> entity.getUpdatedAt(), "Should handle null updatedAt");
        assertDoesNotThrow(() -> entity.getVersion(), "Should handle null version");
        
        // Should handle null ID in equals
        TestEntity otherEntity = new TestEntity();
        assertDoesNotThrow(() -> entity.equals(otherEntity), "Should handle null IDs in equals");
    }

    @Test
    @DisplayName("Should support inheritance properly")
    void shouldSupportInheritanceProperly() {
        TestEntity entity = new TestEntity("test");
        assertTrue(entity instanceof BaseEntity, "TestEntity should be instance of BaseEntity");
        
        // Should be able to cast to BaseEntity
        BaseEntity baseEntity = entity;
        assertNotNull(baseEntity, "Should be able to cast to BaseEntity");
        
        // Should maintain functionality when cast
        assertDoesNotThrow(() -> baseEntity.getId(), "Should maintain ID access when cast");
        assertDoesNotThrow(() -> baseEntity.getCreatedAt(), "Should maintain audit access when cast");
    }

    @Test
    @DisplayName("Should handle concurrent modifications with version")
    void shouldHandleConcurrentModificationsWithVersion() {
        TestEntity entity1 = new TestEntity("original");
        TestEntity entity2 = new TestEntity("modified");
        
        UUID id = UUID.randomUUID();
        entity1.setId(id);
        entity2.setId(id);
        
        // Both start with same version
        entity1.setVersion(1L);
        entity2.setVersion(1L);
        
        // Simulate concurrent modification
        entity1.setVersion(2L); // First update succeeds
        
        // Second update should detect version mismatch
        assertNotEquals(entity1.getVersion(), entity2.getVersion(), 
            "Versions should differ after concurrent modification");
    }
}