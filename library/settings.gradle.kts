rootProject.name = "fluxflow"

// Flux Flow
include("api")
include("stereotyped")

include("engine")
include("reflection")

include("persistence")
include("test-persistence")
include("mongo")

include("scheduling")
include("test-scheduling")
include("springboot-quartz")

include("springboot")
include("springboot-web")
include("springboot-in-memory-persistence")
include("springboot-test-scheduling")
include("testing")

include("query")
include("validation")

include("memorycache")