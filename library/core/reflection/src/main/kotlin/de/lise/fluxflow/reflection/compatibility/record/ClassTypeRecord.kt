package de.lise.fluxflow.reflection.compatibility.record

import de.lise.fluxflow.reflection.compatibility.reference.TypeReference

data class ClassTypeRecord(
    override val name: String,
    val modifiers: Int,
    val isEnum: Boolean,
    val isInterface: Boolean,
    val isRecord: Boolean,
    val isPrimitive: Boolean,
    val isAnnotation: Boolean,
    val isArray: Boolean,
    val constructors: List<ConstructorRecord>,
    val methods: List<MethodRecord>,
    val implementedInterfaces: List<TypeReference>,
    val fields: List<FieldRecord>,
): TypeRecord {
    var superType: TypeReference? = null
        internal set

    var componentType: TypeReference? = null
        internal set
}
