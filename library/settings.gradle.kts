rootProject.name = "fluxflow"

// Flux Flow
include("core:api")
include("core:stereotyped")

include("core:engine")
include("core:reflection")

include("core:persistence")
include("core:test-persistence")
include("springboot:springboot-mongo")

include("core:scheduling")
include("core:test-scheduling")
include("springboot:springboot-quartz")

include("springboot:springboot")
include("springboot:springboot-web")
include("springboot:springboot-in-memory-persistence")
include("springboot:springboot-test-scheduling")
include("springboot:springboot-testing")

include("core:query")
include("core:validation")