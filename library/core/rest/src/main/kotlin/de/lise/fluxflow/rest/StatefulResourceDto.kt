package de.lise.fluxflow.rest

interface StatefulResourceDto<
    TMetadata: ResourceMetadataDto,
    TSpec : ResourceSpecDto,
    TStatus : ResourceStatusDto
> : ResourceDto<TMetadata, TSpec> {
    val status: TStatus
}