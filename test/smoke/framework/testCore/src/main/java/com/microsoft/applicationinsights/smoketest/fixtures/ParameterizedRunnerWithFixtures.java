package com.microsoft.applicationinsights.smoketest.fixtures;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.TestWithParameters;

public class ParameterizedRunnerWithFixtures extends BlockJUnit4ClassRunnerWithParameters {
    private final Object[] parameters;
    public ParameterizedRunnerWithFixtures(TestWithParameters twp) throws InitializationError {
        super(twp);
        this.parameters = twp.getParameters().toArray(new Object[twp.getParameters().size()]);
    }

    private Statement wrapWithParamBefores(final Statement s) {
        final List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(BeforeWithParams.class);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                for (FrameworkMethod fm : methods) {
                    fm.invokeExplosively(null, ParameterizedRunnerWithFixtures.this.parameters);
                }
                s.evaluate();
            }
        };
    }

    private Statement wrapWithParamAfters(final Statement s) {
        final List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(AfterWithParams.class);
        return new Statement() {
			@Override
			public void evaluate() throws Throwable {
                final List<Throwable> errs = new ArrayList<Throwable>();
                try {
                    s.evaluate();
                } catch (Throwable t) {
                    errs.add(t);
                } finally {
                    for (FrameworkMethod fm : methods) {
                        try {
                            fm.invokeExplosively(null, ParameterizedRunnerWithFixtures.this.parameters);
                        } catch (Throwable t) {
                            errs.add(t);
                        }
                    }
                }
                MultipleFailureException.assertEmpty(errs);
			}
        };
    }

    @Override
    protected Statement classBlock(RunNotifier notifier) {
        return wrapWithParamAfters(wrapWithParamBefores(super.classBlock(notifier)));
    }

    @Override
    protected void collectInitializationErrors(List<Throwable> errors) {
        super.collectInitializationErrors(errors);
        validateFixtures(errors);
    }

    private void validateFixtures(List<Throwable> errors) {
        validateFixture(BeforeWithParams.class, errors);
        validateFixture(AfterWithParams.class, errors);
    }

    private void validateFixture(Class<? extends Annotation> annotation, List<Throwable> errors) {
        final List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(annotation);
        for (FrameworkMethod fm : methods) {
            fm.validatePublicVoid(true, errors);
        }
    }
}