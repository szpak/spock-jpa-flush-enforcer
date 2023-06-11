package pl.solidsoft.spock.flush

import spock.lang.Specification
import spock.util.EmbeddedSpecCompiler
import spock.util.EmbeddedSpecRunner

abstract class EmbeddedSpecification extends Specification {

    protected EmbeddedSpecRunner runner = new EmbeddedSpecRunner()
    protected EmbeddedSpecCompiler compiler = new EmbeddedSpecCompiler()

}
