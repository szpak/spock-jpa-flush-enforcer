package pl.solidsoft.spock.flush;

import org.spockframework.runtime.GroovyRuntimeUtil;
import org.spockframework.runtime.extension.AbstractMethodInterceptor;
import org.spockframework.runtime.extension.IAnnotationDrivenExtension;
import org.spockframework.runtime.extension.IBlockListener;
import org.spockframework.runtime.extension.IMethodInvocation;
import org.spockframework.runtime.model.BlockInfo;
import org.spockframework.runtime.model.BlockKind;
import org.spockframework.runtime.model.FieldInfo;
import org.spockframework.runtime.model.IterationInfo;
import org.spockframework.runtime.model.SpecInfo;
import org.spockframework.util.Nullable;
import org.spockframework.util.ReflectionUtil;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public class EnforceJpaSessionFlushExtension implements IAnnotationDrivenExtension<EnforceJpaSessionFlush> {

    private static final Class<?> ENTITY_MANAGER_JAKARTA = ReflectionUtil.loadClassIfAvailable("jakarta.persistence.EntityManager");
    private static final Class<?> ENTITY_MANAGER_JAVAX = ReflectionUtil.loadClassIfAvailable("javax.persistence.EntityManager");
    private static final Class<?> TEST_ENTITY_MANAGER_SPRING = ReflectionUtil.loadClassIfAvailable("org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager");
    private static final Class<?> JPA_REPOSITORY = ReflectionUtil.loadClassIfAvailable("org.springframework.data.jpa.repository.JpaRepository");
    //TODO: Also my own TestEntityManager for own testing? To simplify dependencies in unit tests (on the other hand PersistenceManager is just an interface from separate API package)

    private static final List<Class<?>> SUPPORTED_FLUSHABLE_CLASSES =
            Stream.of(ENTITY_MANAGER_JAKARTA, ENTITY_MANAGER_JAVAX, TEST_ENTITY_MANAGER_SPRING, JPA_REPOSITORY)
                    .filter(Objects::nonNull)
                    .collect(collectingAndThen(toList(), Collections::unmodifiableList)); //could be simplified with just Stream.toList() with JDK 10+

    private static final ThreadLocal<IMethodInvocation> methodInvocationContext = new ThreadLocal<>();

    @Override
    public void visitSpecAnnotation(EnforceJpaSessionFlush annotation, SpecInfo spec) {

        FieldInfo flushableFieldInfo = findFlushableFieldInfo(spec);
        if (flushableFieldInfo == null) {
            throw new FlushExtensionSpockException(format("No flushable field found in %s class annotated with @%s. Supported flushable types: %s",
                    spec.getName(), EnforceJpaSessionFlush.class.getSimpleName(), SUPPORTED_FLUSHABLE_CLASSES));
        }


        //TODO: support super specifications
        spec.getFeatures().forEach(featureInfo -> {
            System.out.println("adding interceptor for: " + featureInfo.getSpec().getName() + "." + featureInfo.getName()); //TODO: Switch to some API wrapper logging only if extension debug is enabled

            IBlockListener whenExitedBlockListener = createWhenExitedBlockListener(flushableFieldInfo);
            featureInfo.addBlockListener(whenExitedBlockListener);

            featureInfo.addIterationInterceptor(new AbstractMethodInterceptor() {
                @Override
                public void interceptIterationExecution(IMethodInvocation invocation) throws Throwable {
                    System.out.println("========== IE " + invocation.getIteration().getIterationIndex() + ", BlockListeners for feature: " + invocation.getFeature().getBlockListeners());

                    try {
                        methodInvocationContext.set(invocation);
                        invocation.proceed();

                    } finally {
                        methodInvocationContext.remove();
                    }
                }
            });
        });
    }

    @Nullable
    private FieldInfo findFlushableFieldInfo(SpecInfo spec) {
        if (SUPPORTED_FLUSHABLE_CLASSES.isEmpty()) {
            return null;
        }

        return spec.getAllFields().stream()
                .filter(fieldInfo -> isAssignableFromAnySupported(fieldInfo.getType()))
                .findFirst()
                .orElse(null);  //TODO
    }

    private boolean isAssignableFromAnySupported(Class<?> fieldType) {
        return SUPPORTED_FLUSHABLE_CLASSES.stream()
                .filter(Objects::nonNull)
                .anyMatch(fieldType::isAssignableFrom);
    }

    private static IBlockListener createWhenExitedBlockListener(FieldInfo flushableFieldInfo) {
        return new IBlockListener() {
            @Override
            public void blockExited(IterationInfo iterationInfo, BlockInfo blockInfo) {

                IMethodInvocation invocation = methodInvocationContext.get();
                if (invocation == null) {
                    throw new FlushExtensionSpockException("Invocation should not be null in ThreadLocal on WhenExitedBlockListener.blockExisted(). " +
                            "Possible bug in EnforceJpaSessionFlushExtension.");    //TODO: Generate debug info?
                }
                System.out.println("II: " + invocation.getIteration().getIterationIndex() + ", " + iterationInfo.getIterationIndex());
                if (invocation.getIteration().getIterationIndex() != iterationInfo.getIterationIndex()) {
                    throw new FlushExtensionSpockException(format("BlockListener executed not for its own iteration: %d != %d. " +
                            "Probably bug in EnforceJpaSessionFlushExtension.",
                            invocation.getIteration().getIterationIndex(), iterationInfo.getIterationIndex()));
                }
                if (blockInfo.getKind() != BlockKind.WHEN) {
                    System.out.println("Not WHEN block, ignoring " + blockInfo.getKind());
                    return;
                }

                System.out.println("Invocation: " + invocation.getSpec().getName() + "." + invocation.getFeature().getName() + ": " + iterationInfo.getIterationIndex());
                System.out.println("I ---- Block exited - iteration - " + blockInfo.getKind());

                Object entityManager = flushableFieldInfo.readValue(invocation.getInstance());
                if (entityManager != null) {
                    //TODO: Error checking?
                    GroovyRuntimeUtil.invokeMethod(entityManager, "flush");
                } else {
                    throw new FlushExtensionSpockException(flushableFieldInfo.getName() + " instance is null :-/");
                }
            }
        };
    }

    /*
      TODO:
       - slf4j API for (optional) debug logging?

     */
}
