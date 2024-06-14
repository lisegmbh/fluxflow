package de.lise.fluxflow.reflection.record

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.function.Predicate

class RecordBuilder(
    private val typeFilter: Predicate<Class<*>> = Predicate { _ -> true }
) {
    private val records: MutableMap<String, TypeRecord> = mutableMapOf()
    private val types: MutableMap<String, Class<*>> = mutableMapOf()

    val allRecords: Map<String, TypeRecord>
        get() {
            return records
        }
    
    fun getTypeRecord(type: Class<*>): TypeRecord? {
        val reference = buildType(type)
        return records[reference.name]
    }
    
    fun getTypeRecord(typeReference: TypeReference): TypeRecord? {
        return records[typeReference.name]
    }

    fun getType(typeReference: TypeReference): Class<*> {
        return types[typeReference.name]!!
    }

    fun getType(typeReference: TypeRecord): Class<*> {
        return types[typeReference.name]!!
    }

    private fun buildType(type: Class<*>): TypeReference {
        val qualifiedName = type.canonicalName
            ?: throw IllegalArgumentException("Can not create records for anonymous type: $type")

        if (!typeFilter.test(type)) {
            return TypeReference(qualifiedName)
        }

        val existing = records[qualifiedName]
        if (existing != null) {
            return TypeReference(existing.name)
        }

        val methods = mutableListOf<MethodRecord>()
        val interfaces = mutableListOf<TypeReference>()
        val fields = mutableListOf<FieldRecord>()
        val constructors = mutableListOf<ConstructorRecord>()
        val typeRecord = TypeRecord(
            qualifiedName,
            type.modifiers,
            type.isEnum,
            type.isInterface,
            type.isRecord,
            type.isPrimitive,
            type.isAnnotation,
            type.isArray,
            constructors,
            methods,
            interfaces,
            fields
        )
        records[qualifiedName] = typeRecord
        types[qualifiedName] = type

        type.superclass?.let {
            typeRecord.superType = buildType(it)
        }
        type.componentType?.let {
            typeRecord.componentType = buildType(it)
        }
        methods.addAll(
            type.declaredMethods.map { buildMethod(it) }
        )
        interfaces.addAll(
            type.interfaces.map { buildType(it) }
        )
        fields.addAll(
            type.declaredFields.map { buildField(it) }
        )
        constructors.addAll(
            type.constructors.map { buildConstructor(it) }
        )

        return TypeReference(qualifiedName)
    }

    private fun buildConstructor(constructor: Constructor<*>): ConstructorRecord {
        return ConstructorRecord(
            constructor.parameters.map { buildParameter(it) }
        )
    }

    private fun buildField(field: Field): FieldRecord {
        return FieldRecord(
            buildType(field.type),
            field.name,
            field.modifiers,
            field.isEnumConstant
        )
    }

    private fun buildMethod(method: Method): MethodRecord {
        return MethodRecord(
            method.name,
            method.returnType?.let { buildType(it) },
            method.modifiers,
            method.isBridge,
            method.isDefault,
            method.parameters.map {
                buildParameter(it)
            }
        )
    }

    private fun buildParameter(parameter: Parameter): ParameterRecord {
        return ParameterRecord(
            parameter.name,
            buildType(parameter.type),
            parameter.modifiers,
            parameter
        )
    }
}