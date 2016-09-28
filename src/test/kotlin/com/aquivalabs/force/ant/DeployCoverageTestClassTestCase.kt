package com.aquivalabs.force.ant

import org.testng.annotations.Test
import org.testng.Assert.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.xmlmatchers.XmlMatchers.*
import org.xmlmatchers.namespace.SimpleNamespaceContext
import org.xmlmatchers.transform.XmlConverters.*
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class DeployCoverageTestClassTestCase {

    private val metadataApiNamespace = SimpleNamespaceContext()
        .withBinding("x", "http://soap.sforce.com/2006/04/metadata")

    @Test fun generateTestClassName_always_shouldReturnStringWithNoMoreThan40Chars() =
        assertTrue(
            generateTestClassName().length <= 40,
            "Apex Class name should be less or equal 40 characters")

    @Test fun generateTestClassName_withSetOfExistingClassNames_shouldReturnStringNotFromSet() {
        val existingClasses = generateClassNames()
        val actual = generateTestClassName(existingClasses)
        assertFalse(
            existingClasses.contains(actual),
            "Should return non-existent class name")
    }

    @Test fun generateTestClass_always_shouldIncludeInputValuesInOutputApexCode() {
        val expectedClassName = generateTestClassName()
        val expectedClassesToTouch = generateClassNames()

        val actual = generateTestClass(expectedClassName, expectedClassesToTouch)

        assertThat(actual, containsString("private class $expectedClassName"))
        assertThat(
            actual,
            containsString(
                expectedClassesToTouch.joinToString(transform = { it -> "'$it'" })))
    }

    @Test fun generateTestClassMetadata_always_shouldReturnWellFormedXml() {
        val expectedVersion = 37.0
        val actual = generateTestClassMetadata(expectedVersion)
        assertThat(
            the(actual),
            hasXPath("/x:ApexClass/x:apiVersion", metadataApiNamespace, equalTo("$expectedVersion")))
    }

    @Test fun generateDestructiveChanges_always_shouldReturnWellFormedXml() {
        val expectedClassName = generateTestClassName()
        val expectedVersion = 37.0

        val actual = generateDestructiveChanges(expectedClassName, expectedVersion)

        assertThat(
            the(actual),
            hasXPath("/x:Package/x:types/x:members", metadataApiNamespace, equalTo(expectedClassName)))
        assertThat(
            the(actual),
            hasXPath("/x:Package/x:types/x:name", metadataApiNamespace, equalTo("ApexClass")))
        assertThat(
            the(actual),
            hasXPath("/x:Package/x:version", metadataApiNamespace, equalTo("$expectedVersion")))
    }

    @Test fun generatePackage_always_shouldReturnWellFormedXml() {
        val expectedVersion = 37.0
        val actual = generatePackage(expectedVersion)
        assertThat(
            the(actual),
            hasXPath("/x:Package/x:version", metadataApiNamespace, equalTo("$expectedVersion")))
    }

    @Test fun stringToCodeNameElement_always_shouldReturnCodeNameElementWithProperText() {
        val expected = generateTestClassName()
        val actual = expected.toCodeNameElement()
        assertEquals(actual.text, expected)
    }

    @Test fun documentSaveToString_always_shouldReturnWellFormedStringEquivalent() {
        val expected = generateDestructiveChanges(generateTestClassName(), 37.0)
        val sut = parseXml(expected.toByteArray())
        val actual = sut.saveToString()
        assertThat(the(actual), isEquivalentTo(the(expected)))
    }

    @Test fun zipFileContainsEntry_always_shouldReturnExpectedResult() {
        withTestDirectory { testDirectory ->
            val expectedEntry = randomString()
            val expectedContent = randomString()
            val file = File(testDirectory, "foobar.zip")

            val fileStream = FileOutputStream(file)
            ZipOutputStream(fileStream).use {
                it.addEntry(expectedEntry, expectedContent)
            }
            val zipFile = ZipFile(file)

            assertTrue(zipFile.containsEntry(expectedEntry), "Should return true for existing entry")
            assertFalse(zipFile.containsEntry(randomString()), "Should return false for non-existing entry")

            val entry = zipFile.getEntry(expectedEntry)
            zipFile.getInputStream(entry).use {
                val actualContent = String(it.readBytes())
                assertEquals(actualContent, expectedContent)
            }
        }
    }

    fun generateClassNames(): Set<String> = setOf(
        generateTestClassName(),
        generateTestClassName(),
        generateTestClassName())
}
