package eu.kanade.tachiyomi.extension

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AggregateCatalogPolicyTest {
    @Test
    fun `runtime addresses allow ip http and https without embedded credentials`() {
        assertEquals("http://192.0.2.1/index.json", AggregateCatalogPolicy.validateUrl(" http://192.0.2.1/index.json "))
        assertEquals("https://example.org/index.json", AggregateCatalogPolicy.validateUrl("https://example.org/index.json"))
        for (address in listOf("file:///private/source", "javascript:alert(1)", "https://user:password@example.org/index.json", "http://example.org/index.json#fragment")) {
            assertThrows(IllegalArgumentException::class.java) { AggregateCatalogPolicy.validateUrl(address) }
        }
    }

    @Test
    fun `automatic refresh refuses a changed signing identity`() {
        val original = "ab".repeat(32)
        AggregateCatalogPolicy.checkSigningKey("", original)
        AggregateCatalogPolicy.checkSigningKey(original, original.uppercase())
        assertThrows(IllegalArgumentException::class.java) {
            AggregateCatalogPolicy.checkSigningKey(original, "cd".repeat(32))
        }
        assertThrows(IllegalArgumentException::class.java) {
            AggregateCatalogPolicy.checkSigningKey("", "unsigned")
        }
    }
}
