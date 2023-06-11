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
