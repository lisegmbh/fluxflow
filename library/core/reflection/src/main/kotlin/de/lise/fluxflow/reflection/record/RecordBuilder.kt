package de.lise.fluxflow.reflection.record

import java.lang.reflect.*
import java.util.function.Predicate

class RecordBuilder(
    private val typeFilter: Predicate<Type> = Predicate { _ -> true }
) {
    private val records: MutableMap<String, TypeRecord> = mutableMapOf()
    private val types: MutableMap<String, Type> = mutableMapOf()

    val allRecords: Map<String, TypeRecord>
        get() {
            return records
        }

    fun getTypeRecord(type: Type): TypeRecord {
        val reference = buildType(type)
        return records[reference.name]!!
    }

    fun getTypeRecord(recordedTypeReference: RecordedTypeReference): TypeRecord {
        return records[recordedTypeReference.name]!!
    }

    fun getType(recordedTypeReference: RecordedTypeReference): Type {
        return types[recordedTypeReference.name]!!
    }

    fun getType(typeReference: ClassTypeRecord): Type {
        return types[typeReference.name]!!
    }

    private fun buildType(type: Type): TypeReference {
        if (!typeFilter.test(type)) {
            return IgnoredTypeReference(type.typeName)
        }
        val existing = records[type.typeName]
        if (existing != null) {
            return RecordedTypeReference(existing.name)
        }

        return when (type) {
            is Class<*> -> {
                buildClassType(type)
            }
            is ParameterizedType -> {
                buildParameterizedType(type)
            }
            is TypeVariable<*> -> {
                buildTypeVariable(type)
            }
            is WildcardType -> {
                buildWildcardType(type)
            }
            is GenericArrayType -> {
                buildGenericArrayType(type)
            }
            else -> {
                throw IllegalArgumentException("Unable to record unknown type: ${type.typeName}")
            }
        }
    }

    private fun buildGenericArrayType(type: GenericArrayType): GenericArrayTypeReference {
        return GenericArrayTypeReference(
            type.typeName,
            buildType(type.genericComponentType)
        )
    }

    private fun buildWildcardType(type: WildcardType): WildcardTypeReference {
        return WildcardTypeReference(
            type.typeName,
            type.lowerBounds.map { buildType(it) },
            type.upperBounds.map { buildType(it) }
        )
    }

    private fun buildTypeVariable(type: TypeVariable<*>): TypeVariableReference {
        return TypeVariableReference(
            type.name,
            type.bounds.map { buildType(it) }
        )
    }

    private fun buildParameterizedType(type: ParameterizedType): RecordedTypeReference {
        val typeArguments = mutableListOf<TypeReference>()

        val typeRecord = ParameterizedTypeRecord(
            type.typeName,
            typeArguments
        )
        records[type.typeName] = typeRecord
        types[type.typeName] = type

        typeRecord.rawType = buildType(type.rawType)
        typeArguments.addAll(
            type.actualTypeArguments.map { buildType(it) }
        )

        return RecordedTypeReference(type.typeName)
    }

    private fun buildClassType(classType: Class<*>): RecordedTypeReference {
        val qualifiedName = classType.typeName
        val methods = mutableListOf<MethodRecord>()
        val interfaces = mutableListOf<TypeReference>()
        val fields = mutableListOf<FieldRecord>()
        val constructors = mutableListOf<ConstructorRecord>()
        val classTypeRecord = ClassTypeRecord(
            qualifiedName,
            classType.modifiers ?: 0,
            classType.isEnum ?: false,
            classType.isInterface ?: false,
            classType.isRecord ?: false,
            classType.isPrimitive ?: false,
            classType.isAnnotation ?: false,
            classType.isArray ?: false,
            constructors,
            methods,
            interfaces,
            fields
        )
        records[qualifiedName] = classTypeRecord
        types[qualifiedName] = classType

        classType.superclass?.let {
            classTypeRecord.superType = buildType(it)
        }
        classType.componentType?.let {
            classTypeRecord.componentType = buildType(it)
        }
        methods.addAll(
            classType.declaredMethods?.map { buildMethod(it) } ?: emptyList()
        )
        interfaces.addAll(
            classType.interfaces?.map { buildType(it) } ?: emptyList()
        )
        fields.addAll(
            classType.declaredFields?.map { buildField(it) } ?: emptyList()
        )
        constructors.addAll(
            classType.constructors?.map { buildConstructor(it) } ?: emptyList()
        )
        return RecordedTypeReference(qualifiedName)

    }

    private fun buildConstructor(constructor: Constructor<*>): ConstructorRecord {
        return ConstructorRecord(
            constructor.parameters.map { buildParameter(it) }
        )
    }

    private fun buildField(field: Field): FieldRecord {
        val genType = field.genericType
        System.out.println(genType.typeName)
        //(field.genericType as ParameterizedType).actualTypeArguments
        return FieldRecord(
            buildType(field.genericType),
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
            buildType(parameter.parameterizedType),
            parameter.modifiers,
            parameter
        )
    }
}