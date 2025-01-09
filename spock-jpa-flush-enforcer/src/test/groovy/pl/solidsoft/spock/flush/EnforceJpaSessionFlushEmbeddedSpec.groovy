package pl.solidsoft.spock.flush

import jakarta.persistence.EntityManager
import org.spockframework.runtime.SpockException
import spock.lang.PendingFeature

class EnforceJpaSessionFlushEmbeddedSpec extends EmbeddedSpecification {

    void setup() {
        runner.addClassImport(EnforceJpaSessionFlush)
    }

    void "should fail with meaningful error if no supported flushable field found"() {
        when:
            runner.runWithImports(
                    """
          @EnforceJpaSessionFlush
          class A extends Specification {
            String someStringField = "ignored"

            def testWithWhenAndThen() {
              when: 1
              then: 1
            }
          }
        """)
        then:
            SpockException e = thrown()
            e.message.contains("No flushable field found")
            e.message.contains("A class")
            e.message.contains("@EnforceJpaSessionFlush")
            e.message.contains("jakarta.persistence.EntityManager")
    }

    @PendingFeature(reason = "Probably allow to point one field with some annotation")
    void "should fail with meaningful error if more than one flushable field is found"() {}

    @PendingFeature //TODO: Which one to implement?
    void "should execute flush on all found fields"() {}

    void "should use flushable field in super class (#description)"() {
        given:
            runner.addClassImport(EntityManager)
        when:
            def result = runner.runWithImports("""

          ${superClassAnnotationString}  
          abstract class SuperA extends Specification {
            protected EntityManager entityManager = Mock()
          }
          
          ${classAnnotationString}  
          class A extends SuperA {

            def testWithWhenAndThen() {
              when:
                1
              then:
                1 * entityManager.flush()
            }
          }
            """)
        then:
            result.testsSucceededCount == 1
        where:
            superClassAnnotationString              | classAnnotationString                   | description
            ""                                      | "@${EnforceJpaSessionFlush.simpleName}" | "ann in class"
            "@${EnforceJpaSessionFlush.simpleName}" | ""                                      | "ann in super class"
            "@${EnforceJpaSessionFlush.simpleName}" | "@${EnforceJpaSessionFlush.simpleName}" | "ann in both classes"
    }

    //TODO: Would @ in super class find also all flushable fields in bottom specs? - no?
    //      Maybe better to just look for the fields in the current class (and also all super classes?)? - somehow asymetric :-/

    //TODO: Would @ in super class adds listener to all features from subspecs? - rather yes - field "might" be accessible there (unless private)
    //      Would @ in class adds listener to all features in superspecs? - rather not - flushable field can be inaccesible there

    void "should not execute flush also in feature from super class"() {
        given:
            runner.addClassImport(EntityManager)
        when:
            def result = runner.runWithImports("""
          abstract class SuperA extends Specification {
            protected EntityManager entityManager = Mock()
            
            def testWithWhenAndThenInSuperClass() {
              when:
                1
              then:
                0 * entityManager.flush()
            }
          }
          
          @EnforceJpaSessionFlush
          class A extends SuperA {

            def testWithWhenAndThen() {
              when:
                1
              then:
                1 * entityManager.flush()
            }
          }
            """)
        then:
            result.testsSucceededCount == 2
    }

    void "should not call flush twice if extension applied more than one in hierarchy"() {
        given:
            runner.addClassImport(EntityManager)
        when:
            def result = runner.runWithImports("""
          @EnforceJpaSessionFlush
          abstract class SuperA extends Specification {
            protected EntityManager entityManager = Mock()
            
            def testWithWhenAndThenInSuperClass() {
              when:
                1
              then:
                1 * entityManager.flush()
            }
          }
          
          @EnforceJpaSessionFlush
          class A extends SuperA {

            def testWithWhenAndThen() {
              when:
                1
              then:
                1 * entityManager.flush()
            }
          }
            """)
        then:
            result.testsSucceededCount == 2
    }

    void "should fail with meaningful error if flushable field instance set to null "() {
        given:
            runner.addClassImport(EntityManager)
        when:
            runner.runWithImports(
                    """
        @EnforceJpaSessionFlush
        class A extends Specification {
          EntityManager em = null

          def testWithWhenAndThen() {
            when: 1
            then: 1
          }
        }
      """)
        then:
            SpockException e = thrown()
            e.message.contains("em instance is null")
    }

}
