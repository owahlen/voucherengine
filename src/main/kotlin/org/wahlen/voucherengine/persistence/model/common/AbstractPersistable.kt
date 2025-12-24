package org.wahlen.voucherengine.persistence.model.common

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.domain.Persistable
import org.springframework.data.util.ProxyUtils
import java.util.*

/**
 * Abstract base class for entities. Provides a consistent implementation of `equals`, `hashCode`, and `toString` methods.
 * Implements Persistable to allow manual ID assignment before persist.
 */
@MappedSuperclass
abstract class AbstractPersistable : Persistable<UUID> {

    /**
     * Unique database identifier for the entity.
     */
    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false, unique = true)
    private var id: UUID? = null
    
    @Transient
    private var isNewEntity: Boolean = true

    override fun getId(): UUID? = id
    
    fun setId(id: UUID?) {
        this.id = id
    }
    
    override fun isNew(): Boolean = isNewEntity
    
    @PostPersist
    @PostLoad
    private fun markAsNotNew() {
        isNewEntity = false
    }

    /**
     * Returns a string representation of the entity.
     *
     * @return A string in the format 'Entity of type [class name] with id: [id]'.
     */
    override fun toString(): String {
        return String.format("Entity of type %s with id: %s", this.javaClass.name, this.id)
    }

    /**
     * Checks if this entity is equal to another object.
     *
     * @param other The object to compare with.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != ProxyUtils.getUserClass(other)) {
            return false
        }
        val that = other as AbstractPersistable
        return Objects.equals(this.id, that.id)
    }

    /**
     * Returns the hash code of the entity.
     *
     * @return The hash code of the entity.
     */
    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}
