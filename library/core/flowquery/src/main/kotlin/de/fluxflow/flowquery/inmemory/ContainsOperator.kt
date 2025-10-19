package de.fluxflow.flowquery.inmemory


data class ContainsOperator<TRoot>(
    private val value: InMemoryOperation<TRoot, String>,
    private val substring: InMemoryOperation<TRoot, String>,
    private val ignoreCasing: Boolean
) : InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return value.execute(input)?.indexOf(
            string = substring.execute(input) ?: return null,
            startIndex = 0,
            ignoreCase = true
        )?.let {
            it != -1
        }
    }
}