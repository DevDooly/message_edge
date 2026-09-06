package com.devdooly.notificationedge

import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** 언어가 추가되거나 문구가 변경될 때 번역 누락과 서식 인자 불일치를 방지한다. */
class LocalizationResourceTest {

    private val resourcesDirectory: File
        get() = listOf(File("src/main/res"), File("app/src/main/res"))
            .firstOrNull { it.isDirectory }
            ?: error("앱 리소스 디렉터리를 찾을 수 없습니다: ${System.getProperty("user.dir")}")

    @Test
    fun `영어 기본값과 한국어 번역의 문자열 및 복수형 키가 일치한다`() {
        val english = readResources("values")
        val korean = readResources("values-ko")

        assertTrue("검증할 영어 리소스가 있어야 합니다", english.isNotEmpty())
        assertEquals("영어와 한국어 번역 키가 다릅니다", english.keys, korean.keys)
    }

    @Test
    fun `번역문은 같은 위치와 타입의 서식 인자를 사용한다`() {
        val english = readResources("values")
        val korean = readResources("values-ko")
        assertEquals(english.keys, korean.keys)

        english.forEach { (key, source) ->
            val translated = korean.getValue(key)
            assertEquals("$key formatted 속성이 다릅니다", source.formatted, translated.formatted)
            if (source.kind == "plurals") {
                assertTrue("$key 영어 other 수량이 필요합니다", source.variants.containsKey("other"))
                assertTrue("$key 한국어 other 수량이 필요합니다", translated.variants.containsKey("other"))
            }
            if (source.formatted) {
                // 한국어는 other만 사용한다. 없는 수량은 해당 언어의 other와 비교한다.
                (source.variants.keys + translated.variants.keys).forEach { quantity ->
                    val sourceText = source.variants[quantity] ?: source.variants.getValue("other")
                    val translatedText = translated.variants[quantity] ?: translated.variants.getValue("other")
                    assertEquals(
                        "$key/$quantity 서식 인자의 위치 또는 타입이 다릅니다",
                        placeholderSignature(sourceText),
                        placeholderSignature(translatedText)
                    )
                }
            }
        }
    }

    @Test
    fun `영어 기본 리소스에 번역되지 않은 한글이 남지 않는다`() {
        val koreanScript = Regex("[\\u1100-\\u11FF\\u3130-\\u318F\\uA960-\\uA97F\\uAC00-\\uD7FF]")
        readResources("values").forEach { (key, resource) ->
            resource.variants.forEach { (quantity, value) ->
                assertFalse("$key/$quantity 영어 기본값에 한글이 남아 있습니다: $value", koreanScript.containsMatchIn(value))
            }
        }
    }

    @Test
    fun `앱별 언어 설정에 영어와 한국어를 등록한다`() {
        val localeDocument = readXml(File(resourcesDirectory, "xml/locales_config.xml"))
        val locales = localeDocument.documentElement.getElementsByTagName("locale")
        val languageTags = (0 until locales.length).map { index ->
            (locales.item(index) as Element).getAttributeNS(ANDROID_NAMESPACE, "name")
        }
        assertEquals(setOf("en", "ko"), languageTags.toSet())
        assertEquals("같은 언어를 중복 등록하면 안 됩니다", languageTags.size, languageTags.toSet().size)

        val manifest = readXml(File(resourcesDirectory.parentFile, "AndroidManifest.xml"))
        val application = manifest.getElementsByTagName("application").item(0) as Element
        assertEquals("@xml/locales_config", application.getAttributeNS(ANDROID_NAMESPACE, "localeConfig"))
    }

    @Test
    fun `서식 검사기는 인자 순서 변경과 퍼센트 리터럴을 허용한다`() {
        assertEquals(
            placeholderSignature("%1\$s %2\$d %3\$.1f %% %n"),
            placeholderSignature("%3\$.1f / %2\$d / %1\$s / %%")
        )
        assertEquals(emptyMap<Int, Set<String>>(), placeholderSignature("100%% %n"))
        assertEquals(placeholderSignature("%1\$s %2\$d"), placeholderSignature("%s %d"))
    }

    @Test
    fun `서식 검사기는 누락된 인자와 타입이 바뀐 인자를 구분한다`() {
        assertNotEquals(placeholderSignature("%1\$s %2\$d"), placeholderSignature("%1\$s"))
        assertNotEquals(placeholderSignature("%1\$s"), placeholderSignature("%1\$d"))
        assertEquals(placeholderSignature("%1\$s"), placeholderSignature("%1\$s %<s"))
    }

    private fun readResources(directoryName: String): Map<String, LocalizedResource> {
        val directory = File(resourcesDirectory, directoryName)
        assertTrue("$directoryName 리소스 디렉터리가 필요합니다", directory.isDirectory)
        val result = linkedMapOf<String, LocalizedResource>()
        directory.listFiles().orEmpty().filter { it.extension == "xml" }.sortedBy { it.name }.forEach { file ->
            val nodes = readXml(file).documentElement.childNodes
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as? Element ?: continue
                if (element.tagName !in setOf("string", "plurals")) continue
                if (element.getAttribute("translatable") == "false") continue
                val key = "${element.tagName}/${element.getAttribute("name")}"
                val variants = if (element.tagName == "plurals") {
                    val items = element.getElementsByTagName("item")
                    val quantities = linkedMapOf<String, String>()
                    for (itemIndex in 0 until items.length) {
                        val item = items.item(itemIndex) as Element
                        val quantity = item.getAttribute("quantity")
                        assertFalse("$key 수량 $quantity 중복", quantities.containsKey(quantity))
                        quantities[quantity] = item.textContent
                    }
                    quantities
                } else {
                    mapOf("string" to element.textContent)
                }
                assertFalse("$directoryName/$key 리소스 중복", result.containsKey(key))
                result[key] = LocalizedResource(
                    kind = element.tagName,
                    formatted = element.getAttribute("formatted") != "false",
                    variants = variants
                )
            }
        }
        return result
    }

    private fun placeholderSignature(value: String): Map<Int, Set<String>> {
        val signature = linkedMapOf<Int, MutableSet<String>>()
        var nextImplicitArgument = 1
        var previousArgument: Int? = null
        FORMAT_ARGUMENT.findAll(value).forEach { match ->
            val conversion = match.groupValues[4]
            if (conversion == "%" || conversion == "n") return@forEach
            val explicitArgument = match.groupValues[1].toIntOrNull()
            val flags = match.groupValues[2]
            val argument = when {
                explicitArgument != null -> explicitArgument
                '<' in flags -> requireNotNull(previousArgument) { "재사용할 이전 서식 인자가 없습니다: $value" }
                else -> nextImplicitArgument++
            }
            previousArgument = argument
            val conversionType = if (match.groupValues[3].isNotEmpty()) "date:$conversion" else conversion.lowercase()
            signature.getOrPut(argument) { linkedSetOf() }.add(conversionType)
        }
        return signature
    }

    private fun readXml(file: File) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    }.newDocumentBuilder().parse(file)

    private data class LocalizedResource(
        val kind: String,
        val formatted: Boolean,
        val variants: Map<String, String>
    )

    companion object {
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        private val FORMAT_ARGUMENT = Regex("%(?:(\\d+)\\$)?([-#+ 0,(<]*)(?:\\d+)?(?:\\.\\d+)?([tT])?([a-zA-Z%])")
    }
}
