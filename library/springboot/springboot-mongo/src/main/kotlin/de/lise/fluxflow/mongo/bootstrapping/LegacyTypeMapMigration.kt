package de.lise.fluxflow.mongo.bootstrapping

import de.lise.fluxflow.mongo.MongoFieldNames
import de.lise.fluxflow.mongo.generic.NullType
import de.lise.fluxflow.mongo.generic.SimpleType
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate

/** Converts historical raw type-name maps using the active converter's output schema. */
internal class LegacyTypeMapMigration(private val template: MongoTemplate) {
    private val converter = template.converter
    private val fields = MongoFieldNames(converter.mappingContext)

    fun migrate(type: Class<*>, historicalField: String, typeMapProperty: String, entriesProperty: String) {
        val target = fields.fieldParts(type, typeMapProperty)
        val entries = fields.fieldParts(type, entriesProperty)
        val typeName = fields.fieldParts(SimpleType::class.java, "typeName")
        val nullType = typeDocument(NullType::class.java)
        val simpleType = typeDocument(SimpleType::class.java)
        val simpleExpression = if (typeName.any(::requiresLiteralField)) {
            setPath(literal(simpleType), typeName, "\$\$p.v")
        } else {
            Document("\$mergeObjects", listOf(literal(simpleType), nestedDocument(typeName, "\$\$p.v")))
        }
        val converted = Document("\$arrayToObject", Document("\$map", Document()
            .append("input", Document("\$objectToArray", "\$$historicalField"))
            .append("as", "p")
            .append("in", Document("k", "\$\$p.k").append("v", Document("\$cond", listOf(
                Document("\$eq", listOf("\$\$p.v", null)),
                literal(nullType),
                simpleExpression,
            ))))))
        val filter = Document("\$expr", Document("\$and", listOf(
            Document("\$in", listOf(Document("\$type", readPath(target)), listOf("missing", "null"))),
            Document("\$eq", listOf(Document("\$type", readPath(entries)), "missing")),
        )))
        val update = if (target.any(::requiresLiteralField)) {
            Document("\$replaceWith", setPath("\$\$ROOT", target, converted))
        } else {
            // Ordinary mapped paths retain the pre-existing MongoDB 4.2 update-pipeline support.
            Document("\$set", Document(target.joinToString("."), converted))
        }
        template.getCollection(template.getCollectionName(type)).updateMany(filter, listOf(update))
    }

    private fun typeDocument(type: Class<*>): Document = Document().also { converter.typeMapper.writeType(type, it) }

    private fun literal(value: Any): Document = Document("\$literal", value)

    private fun requiresLiteralField(field: String): Boolean = '.' in field || field.startsWith('$')

    private fun nestedDocument(path: List<String>, value: Any): Any =
        path.asReversed().fold(value) { nested, field -> Document(field, nested) }

    private fun readPath(path: List<String>): Any = if (path.none(::requiresLiteralField)) {
        "\$" + path.joinToString(".")
    } else {
        path.fold("\$\$ROOT" as Any) { input, field ->
            Document("\$let", Document("vars", Document("parent", input)).append("in",
                Document("\$cond", listOf(
                    Document("\$eq", listOf(Document("\$type", "\$\$parent"), "object")),
                    getField("\$\$parent", field),
                    "\$\$REMOVE",
                )),
            ))
        }
    }

    /** Only literal dotted KEY fields and '$' segments need the MongoDB 5.0 field operators. */
    private fun getField(input: Any, field: String): Document =
        Document("\$getField", Document("field", literal(field)).append("input", input))

    private fun setPath(input: Any, path: List<String>, value: Any): Document {
        val field = path.first()
        val nestedValue = if (path.size == 1) value else setPath(
            Document("\$ifNull", listOf(getField(input, field), Document())),
            path.drop(1),
            value,
        )
        return Document("\$setField", Document("field", literal(field))
            .append("input", input).append("value", nestedValue))
    }
}
