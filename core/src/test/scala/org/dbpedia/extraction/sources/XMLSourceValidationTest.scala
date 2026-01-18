package org.dbpedia.extraction.sources

import java.io.File
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.dbpedia.extraction.util.Language

/**
 * Test for XMLSource validation to prevent empty/malformed ontology.xml parsing failures (issue #753)
 */
@RunWith(classOf[JUnitRunner])
class XMLSourceValidationTest extends FlatSpec with Matchers {

  "XMLSource.fromFile" should "throw IllegalArgumentException for non-existent file" in {
    val nonExistentFile = new File("nonexistent_file.xml")
    
    val exception = intercept[IllegalArgumentException] {
      XMLSource.fromFile(nonExistentFile, Language.Mappings)
    }
    
    exception.getMessage should include("does not exist")
  }

  it should "throw IllegalArgumentException for empty file" in {
    val emptyFile = File.createTempFile("empty_ontology", ".xml")
    emptyFile.deleteOnExit()
    
    val exception = intercept[IllegalArgumentException] {
      XMLSource.fromFile(emptyFile, Language.Mappings)
    }
    
    exception.getMessage should include("empty")
    exception.getMessage should include("DBpedia API")
  }

  it should "throw IllegalArgumentException for suspiciously small file" in {
    val smallFile = File.createTempFile("small_ontology", ".xml")
    smallFile.deleteOnExit()
    
    // Write less than 100 bytes
    val writer = new java.io.FileWriter(smallFile)
    writer.write("<xml>small</xml>")
    writer.close()
    
    val exception = intercept[IllegalArgumentException] {
      XMLSource.fromFile(smallFile, Language.Mappings)
    }
    
    exception.getMessage should include("suspiciously small")
    exception.getMessage should (include("bytes") and include("malformed"))
  }

  it should "accept valid XML file with sufficient size" in {
    val validFile = File.createTempFile("valid_ontology", ".xml")
    validFile.deleteOnExit()
    
    // Write a minimal valid MediaWiki XML (>100 bytes)
    val writer = new java.io.FileWriter(validFile)
    writer.write(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<mediawiki xmlns="http://www.mediawiki.org/xml/export-0.10/" version="0.10">
        |  <page>
        |    <title>Test</title>
        |    <ns>0</ns>
        |    <id>1</id>
        |    <revision>
        |      <id>1</id>
        |      <timestamp>2023-01-01T00:00:00Z</timestamp>
        |      <contributor><username>Test</username><id>1</id></contributor>
        |      <text xml:space="preserve">Test content</text>
        |    </revision>
        |  </page>
        |</mediawiki>""".stripMargin)
    writer.close()
    
    // Should not throw exception
    noException should be thrownBy {
      XMLSource.fromFile(validFile, Language.Mappings)
    }
  }
}
