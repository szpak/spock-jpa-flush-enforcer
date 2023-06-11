package pl.solidsoft.spock.flush

import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import jakarta.persistence.EntityManager

@EnforceJpaSessionFlush
class EnforceJpaSessionFlushSpec extends Specification {

    @Autowired
    private EntityManager entityManager = Mock()

    void "foo"() {
        when:
            println "when"

        then:
            println "then"

            1 * entityManager.flush() >> {
                println "=== flushed ==="
            }
    }

    void "foo - parameterized"() {
        when:
            println "when"

        then:
            println "then"

            1 * entityManager.flush() >> {
                println "=== flushed ==="
            }

        where:
            i << [1, 2]
    }

    void "foo - two blocks"() {
        when:
            println "when"

        then:
            println "then"

        and:
            1 * entityManager.flush() >> {
                println "=== flushed ==="
            }

        when:
            println "when2"

        then:
            println "then2"

        and:
            1 * entityManager.flush() >> {
                println "=== flushed ==="
            }
    }

}
