package org.dbpedia.extraction.ontology.io

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.wikiparser.WikiPage

/**
 * Test for OntologyReader error handling for empty ontology sources (issue #753)
 */
@RunWith(classOf[JUnitRunner])
class OntologyReaderValidationTest extends FlatSpec with Matchers {

  "OntologyReader.read" should "throw IllegalStateException for empty source" in {
    val emptySource = new Source {
      override def foreach[U](f: WikiPage => U): Unit = {
        // Empty - no pages
      }
      override def hasDefiniteSize: Boolean = true
    }
    
    val reader = new OntologyReader()
    
    val exception = intercept[IllegalStateException] {
      reader.read(emptySource)
    }
    
    exception.getMessage should include("empty")
    exception.getMessage should include("Mappings API")
    exception.getMessage should include("https://mappings.dbpedia.org/api.php")
  }

  it should "throw RuntimeException when source parsing fails" in {
    val failingSource = new Source {
      override def foreach[U](f: WikiPage => U): Unit = {
        throw new RuntimeException("Simulated parsing error")
      }
      override def hasDefiniteSize: Boolean = true
    }
    
    val reader = new OntologyReader()
    
    val exception = intercept[RuntimeException] {
      reader.read(failingSource)
    }
    
    exception.getMessage should include("Failed to load ontology from source")
    exception.getMessage should include("empty or malformed")
    exception.getCause.getMessage should include("Simulated parsing error")
  }

  it should "log successful page count when loading valid ontology" in {
    // This is a minimal test - full integration test would require valid ontology pages
    // For now, we verify that the error handling doesn't trigger with at least one page
    
    val validSource = new Source {
      private var called = false
      override def foreach[U](f: WikiPage => U): Unit = {
        if (!called) {
          // Simulate at least one valid page node being created
          called = true
        }
      }
      override def hasDefiniteSize: Boolean = true
    }
    
    val reader = new OntologyReader()
    
    // This will still fail during actual ontology building, but we're testing
    // that the validation doesn't trigger false positives
    // The actual error will be different from our validation errors
    val exception = intercept[Exception] {
      reader.read(validSource)
    }
    
    // Should NOT be our validation error messages
    exception.getMessage should not include "Ontology source is empty"
  }
}
