package de.lise.fluxflow.rest

interface ResourceDto<
        out TMetadata : ResourceMetadataDto,
        out TSpec : ResourceSpecDto
> {
    val kind: String
    val metadata: TMetadata
    val spec: TSpec
}