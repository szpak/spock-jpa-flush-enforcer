package pl.solidsoft.spock.flush

import jakarta.persistence.EntityManager
//import javax.persistence.EntityManager as JavaxEntityManager
import org.spockframework.runtime.SpockException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification
import spock.util.EmbeddedSpecCompiler
import spock.util.EmbeddedSpecRunner

class EnforceJpaSessionFlushDifferentClassesSpec extends Specification {

    private EmbeddedSpecRunner runner = new EmbeddedSpecRunner()
    private EmbeddedSpecCompiler compiler = new EmbeddedSpecCompiler()

    void "should detect any supported field type"() {
        given:
            compiler.addClassImport(EnforceJpaSessionFlush)
            compiler.addClassImport(Autowired)
        and:
            def testClass = compiler.compileWithImports(
                    """
          @EnforceJpaSessionFlush
          class A extends Specification {
            static List<String> calls = []

            @Autowired
            $clazz entityManager = Mock()

            def foo() {
              given:
                entityManager.flush() >> { println "flush"; calls.add("flush") }
              when:
                println "when"
                calls.add("when")
              then:
                println "then"
                calls.add("then")
            }
          }
      """
                    , "")[0]

        when:
            def result = runner.runClass(testClass)

        then:
            testClass.calls == ["when", "flush", "then"]
            result.testsSucceededCount == 1

        where:
            clazz << [EntityManager.class.name, /*TODO: javax.EM, */ TestEntityManager.class.name, JpaRepository.class.name]
    }

    //TODO: Move to another class
    void "should skip execution for no supported fields"() {
        given:
            runner.addClassImport(EnforceJpaSessionFlush)
        when:
            def result = runner.runWithImports(
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
            result.testsSucceededCount == 1 //TODO: Or should fail to clearly say "something is wrong"
    }

    void "should fail with meaningful error for EntityManager instance set to null "() {
        given:
            runner.addClassImport(EnforceJpaSessionFlush)
            runner.addClassImport(EntityManager)
        when:
            def result = runner.runWithImports(
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
