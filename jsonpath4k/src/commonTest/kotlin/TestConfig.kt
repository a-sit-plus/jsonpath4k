import at.asitplus.testballoon.matrix.ExecutionMode
import at.asitplus.testballoon.matrix.MatrixTestDefaults
import de.infix.testBalloon.framework.core.TestSession

/**
 * Single [TestSession] for this module. The RFC-conformance suites mutate process-global state
 * (`JsonPath.defaultCompiler`, `JsonPath.defaultFunctionExtensionRepository`), so the default matrix
 * execution is [ExecutionMode.Sequential]. The dedicated ANTLR concurrency stress suite opts into real
 * parallelism explicitly (see the nativeTest source set).
 */
class TestConfig : TestSession(
    testConfig = DefaultConfiguration.apply { MatrixTestDefaults { execution = ExecutionMode.Concurrent(24) } }
)