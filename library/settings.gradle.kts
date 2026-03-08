rootProject.name = "fluxflow"

// Flux Flow
include("core:api")
include("core:stereotyped")

include("core:engine")
include("core:reflection")

include("core:persistence")
include("core:test-persistence")

include("core:scheduling")
include("core:test-scheduling")

include("core:migration")

include("springboot:springboot")
include("springboot:springboot-mongo")
include("springboot:springboot-web")
include("springboot:springboot-quartz")
include("springboot:springboot-in-memory-persistence")
include("springboot:springboot-test-scheduling")
include("springboot:springboot-testing")

include("springboot4:springboot")
include("springboot4:springboot-mongo")
include("springboot4:springboot-web")
include("springboot4:springboot-quartz")
include("springboot4:springboot-in-memory-persistence")
include("springboot4:springboot-test-scheduling")
include("springboot4:springboot-testing")

include("core:query")
include("core:flowquery")
include("core:validation")

include("core:rest")