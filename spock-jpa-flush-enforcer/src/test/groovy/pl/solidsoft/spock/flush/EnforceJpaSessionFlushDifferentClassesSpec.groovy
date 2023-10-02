package pl.solidsoft.spock.flush

import jakarta.persistence.EntityManager

//import javax.persistence.EntityManager as JavaxEntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.jpa.repository.JpaRepository

class EnforceJpaSessionFlushDifferentClassesSpec extends EmbeddedSpecification {

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

}
