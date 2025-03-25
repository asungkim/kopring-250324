package com.example.upload.global.entity

import com.example.upload.standard.util.Ut
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class BaseEntity {
    @Id // PRIMARY KEY
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    private var _id: Long? = null // TODO: 추후에 전환 과정에서 해결 (private->public)

    var id: Long
        get() = _id ?: 0
        set(value) {
            _id = value
        }

    val modelName: String
        get() = Ut.str.lcfirst(this::class.simpleName)

    override fun equals(other: Any?): Boolean {

        if (other == null || javaClass != other.javaClass) return false

        other as BaseEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: System.identityHashCode(this)
    }
}
