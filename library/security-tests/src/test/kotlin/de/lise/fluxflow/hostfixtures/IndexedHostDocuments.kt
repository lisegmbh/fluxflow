package de.lise.fluxflow.hostfixtures

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

// Outside the Boot test configuration's scanned package, so mapping happens during the test.
@Document("host_index_before_access")
internal data class IndexedHostBeforeAccess(@field:Indexed(unique = true) val hostKey: String)

@Document("host_index_after_access")
internal data class IndexedHostAfterAccess(@field:Indexed(unique = true) val hostKey: String)
