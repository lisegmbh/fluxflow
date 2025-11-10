package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.stereotyped.Import
import de.lise.fluxflow.stereotyped.PrefixStrategy
import kotlin.reflect.KProperty1

data class KindPrefixBuilder(
    val strategy: PrefixStrategy,
    val prefix: String,
) {
    fun apply(name: String): String {
        return if (prefix.isBlank()) {
            name
        } else {
            strategy.apply(
                prefix,
                name
            )
        }
    }

    companion object {
        fun KindPrefixBuilder?.and(annotation: Import): KindPrefixBuilder? {
            return when (this) {
                null -> KindPrefixBuilder(
                    annotation.prefixStrategy,
                    annotation.prefix
                )

                else -> KindPrefixBuilder(
                    annotation.prefixStrategy,
                    strategy.apply(
                        prefix,
                        annotation.prefix
                    )
                )
            }
        }

        fun KindPrefixBuilder?.build(prop: KProperty1<*, *>): DataKind {
            val rawKind = DataKindInspector.getDataKind(prop).value
            return DataKind(
                value = this?.apply(rawKind) ?: rawKind
            )
        }
    }
}