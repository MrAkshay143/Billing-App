package com.billing.app.core.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HardcodedStringGuardTest {

    private val textLiteralRegex = Regex("""(?<![a-zA-Z0-9_])Text\s*\(\s*(?:text\s*=\s*)?"([^"]+)"""")

    @Test
    fun testHardcodedStringCountDoesNotExceedBaseline() {
        val cwd = File(".").canonicalFile
        val rootDir = if (File(cwd, "app/src/main/java").exists()) cwd else (cwd.parentFile ?: cwd)
        val mainJavaDir = File(rootDir, "app/src/main/java")
        assertTrue("main/java must exist: ${mainJavaDir.absolutePath}", mainJavaDir.exists())

        // Load baseline JSON
        val baselineFile = File(rootDir, "app/src/test/resources/hardcoded_strings_baseline.json")
        assertTrue("Baseline file must exist: ${baselineFile.absolutePath}", baselineFile.exists())

        val baselineContent = baselineFile.readText()
        val baselineMap = mutableMapOf<String, Int>()

        // Simple JSON parse for {"path": count}
        val entryRegex = Regex(""""([^"]+)":\s*(\d+)""")
        entryRegex.findAll(baselineContent).forEach { match ->
            val path = match.groupValues[1]
            val count = match.groupValues[2].toInt()
            baselineMap[path] = count
        }

        val ktFiles = mainJavaDir.walkTopDown().filter { it.extension == "kt" }.toList()
        val violations = mutableListOf<String>()

        for (file in ktFiles) {
            val relPath = file.relativeTo(rootDir).path.replace('\\', '/')
            val content = file.readText()
            val matches = textLiteralRegex.findAll(content).toList()
            val currentCount = matches.size
            val baselineCount = baselineMap[relPath] ?: 0

            if (currentCount > baselineCount) {
                violations.add("File $relPath exceeded baseline! Expected at most $baselineCount, but found $currentCount (+${currentCount - baselineCount}).")
            }
        }

        assertTrue(
            "Hardcoded string violations found (strings must be centralized in AppStrings):\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun testStringsProviderReturnsValidStrings() {
        assertNotNull(StringsProvider.current)
        assertNotNull(StringsProvider.current.navHome)

        StringsProvider.updateLanguage("bn")
        assertEquals("হোম", StringsProvider.current.navHome)
        assertEquals("কেজি", StringsProvider.current.unitSymbol("KG"))

        StringsProvider.updateLanguage("hi")
        assertEquals("होम", StringsProvider.current.navHome)
        assertEquals("किग्रा", StringsProvider.current.unitSymbol("KG"))

        StringsProvider.updateLanguage("en")
        assertEquals("Home", StringsProvider.current.navHome)
        assertEquals("kg", StringsProvider.current.unitSymbol("KG"))
    }

    @Test
    fun testLanguageParityAcrossAllLanguages() {
        val en = EnglishStrings()
        val bn = BengaliStrings()
        val hi = HindiStrings()

        // Verify critical P1 keys are non-blank across En, Bn, Hi
        assertTrue(en.finishAndBill.isNotBlank())
        assertTrue(bn.finishAndBill.isNotBlank())
        assertTrue(hi.finishAndBill.isNotBlank())

        assertTrue(en.grandTotalLabel.isNotBlank())
        assertTrue(bn.grandTotalLabel.isNotBlank())
        assertTrue(hi.grandTotalLabel.isNotBlank())

        assertTrue(en.openRegisterShift.isNotBlank())
        assertTrue(bn.openRegisterShift.isNotBlank())
        assertTrue(hi.openRegisterShift.isNotBlank())

        assertTrue(en.newGrnTitle.isNotBlank())
        assertTrue(bn.newGrnTitle.isNotBlank())
        assertTrue(hi.newGrnTitle.isNotBlank())

        assertTrue(en.managerPin.isNotBlank())
        assertTrue(bn.managerPin.isNotBlank())
        assertTrue(hi.managerPin.isNotBlank())
    }
}
