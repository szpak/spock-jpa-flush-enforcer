package pl.solidsoft.spock.flush

import jakarta.persistence.EntityManager
import spock.lang.Specification

@EnforceJpaSessionFlush
class EnforceJpaSessionFlushSpec extends Specification {

    private EntityManager entityManager = Mock()
    private List<String> calls = []

    void "foo"() {
        when:
            recordAction("when")
        then:
            recordAction("then")
        and:
            1 * entityManager.flush() >> {
                recordAction("flush")
            }
        then:
            calls == ["when", "flush", "then"]
    }

    void "foo - parameterized"() {
        when:
            recordAction("when")
        then:
            recordAction("then")
        and:
            1 * entityManager.flush() >> {
                recordAction("flush")
            }
        then:
            calls == ["when", "flush", "then"]
        where:
            i << [1, 2]
    }

    void "foo - two blocks"() {
        when:
            recordAction("when")
        then:
            recordAction("then")
        and:
            1 * entityManager.flush() >> {
                recordAction("flush")
            }
        when:
            recordAction("when2")
        then:
            recordAction("then2")
        and:
            1 * entityManager.flush() >> {
                recordAction("flush2")
            }
        then:
            calls == ["when", "flush", "then", "when2", "flush2", "then2"]
    }

    private void recordAction(String actionName) {
        println actionName
        calls.add(actionName)
    }
}
