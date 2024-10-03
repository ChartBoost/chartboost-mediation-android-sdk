package com.chartboost.sdk.test;

import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.util.DisplayMetrics;

import com.chartboost.sdk.legacy.Factory;
import com.chartboost.sdk.mock.android.DisplayMetricsMockWrapper;

import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public class TestFactory extends Factory {
    private final List<InterceptionRule> interceptionRules = new ArrayList<>();

    public <T> void installIntercept(final Class<T> cls, final Object object) {
        //noinspection unchecked
        final T t = (T) object;
        addRule(matchExactClass(cls), replaceWith(t));
    }

    public <T> T interceptedMock(final Class<T> cls) {
        return interceptedMock(cls, RETURNS_DEFAULTS);
    }

    public <T> T interceptedMock(final Class<T> cls, final Answer<Object> defaultAnswer) {
        T replacement = mock(cls, defaultAnswer);

        addRule(matchExactClass(cls), replaceWith(replacement));

        return replacement;
    }

    public <T> void spyOnClass(Class<T> cls) {
        addRule(matchExactClass(cls), spyOnInstance(cls));
    }

    public <T> void spyOnClass(Class<T> cls, SpyInitializer<T> spyInitializer) {
        addRule(matchExactClass(cls), spyOnInstanceWithInitializer(spyInitializer));
    }

    public <T> void spyOnAnySubclassOf(Class<T> cls) {
        addRule(matchSubclass(cls), spyOnInstance(cls));
    }

    public <T> void spyOnAnySubclassOf(Class<T> cls, SpyInitializer<T> spyInitializer) {
        addRule(matchSubclass(cls), spyOnInstanceWithInitializer(spyInitializer));
    }

    public <T> void removeRulesForClass(Class<T> cls) {
        for (Iterator<InterceptionRule> itr = interceptionRules.iterator(); itr.hasNext(); ) {
            InterceptionRule rule = itr.next();
            if (rule.matchesClass(cls))
                itr.remove();
        }
    }

    public <T> T intercept(T intercepted) {
        if (intercepted == null) // this happens in when() calls
            return null;

        T specialCaseReplacement = getSpecialCaseReplacement(intercepted);
        if (specialCaseReplacement != null)
            return specialCaseReplacement;

        for (Iterator<InterceptionRule> itr = interceptionRules.iterator(); itr.hasNext(); ) {
            InterceptionRule rule = itr.next();
            //noinspection unchecked
            if (rule.matchesClass(intercepted.getClass())) {
                //noinspection unchecked
                T interceptor = (T) rule.act(intercepted);

                if (!rule.retainRule)
                    itr.remove();

                cleanupReplacement(intercepted, interceptor);

                return interceptor;
            }
        }

        return intercepted;
    }

    public <T> AtomicReference<T> saveReferenceOnInterceptAnySubClass(Class<T> cls) {
        AtomicReference<T> ref = new AtomicReference<>();

        addRule(matchSubclass(cls), saveReference(ref));

        return ref;
    }

    public <T> AtomicReference<T> spyAndSaveReferenceToClass(Class<T> cls) {
        AtomicReference<T> ref = new AtomicReference<T>();

        addRule(matchExactClass(cls), spyAndSaveReference(ref));

        return ref;
    }

    public <T> void spyOnAllInstances(Class<T> cls) {
        addRule(matchExactClass(cls), spyOnInstance(cls), true);
    }

    private interface Predicate<T> {
        boolean matchesClass(Class cls);
    }

    private interface Action<T> {
        T act(T t);
    }

    private <T> Predicate<T> matchExactClass(final Class<T> expectedClass) {
        return new Predicate<T>() {
            @Override
            public boolean matchesClass(Class cls) {
                return expectedClass == cls;
            }
        };
    }

    private <T> Predicate<T> matchSubclass(final Class<T> expectedClass) {
        return new Predicate<T>() {
            @Override
            public boolean matchesClass(Class cls) {
                return expectedClass.isAssignableFrom(cls);
            }
        };
    }

    private <T> Action<T> replaceWith(final T replacement) {
        return new Action<T>() {

            @Override
            public T act(T t) {
                return replacement;
            }
        };
    }

    private <T> Action<T> spyOnInstance(@SuppressWarnings("UnusedParameters") Class<T> unused) {
        return new Action<T>() {
            @Override
            public T act(T intercepted) {
                return spy(intercepted);
            }
        };
    }

    private <T> Action<T> spyOnInstanceWithInitializer(final SpyInitializer<T> initializer) {
        return new Action<T>() {
            @Override
            public T act(T intercepted) {
                T obj = spy(intercepted);
                initializer.initialize(obj);
                return obj;
            }
        };
    }

    private <T> Action<T> saveReference(final AtomicReference<T> ref) {
        return new Action<T>() {

            @Override
            public T act(T t) {
                ref.set(t);
                return t;
            }
        };
    }

    private <T> Action<T> spyAndSaveReference(final AtomicReference<T> ref) {
        return new Action<T>() {

            @Override
            public T act(T t) {
                T spied = spy(t);
                ref.set(spied);
                return spied;
            }
        };
    }

    private class InterceptionRule<T> {
        final Predicate<T> predicate;
        final Action<T> action;
        final boolean retainRule;

        InterceptionRule(Predicate<T> predicate, Action<T> action, boolean retainRule) {
            this.predicate = predicate;
            this.action = action;
            this.retainRule = retainRule;
        }

        boolean matchesClass(Class cls) {
            return predicate.matchesClass(cls);
        }

        T act(T intercepted) {
            return action.act(intercepted);
        }
    }

    private <T> T getSpecialCaseReplacement(T intercepted) {
        if (intercepted.getClass() == DisplayMetrics.class) {
            DisplayMetricsMockWrapper wrapper = new DisplayMetricsMockWrapper();
            //noinspection unchecked
            return (T) wrapper.mockDisplayMetrics;
        } else {
            return null;
        }
    }

    private <T> void cleanupReplacement(T intercepted, T interceptor) {
        // When we intercept creation of an instance, we are throwing away
        // the actually created instance.  Sometimes there's some cleanup to do:
        if (intercepted instanceof ExecutorService) {
            ExecutorService svc = (ExecutorService) intercepted;
            svc.shutdown();
        }
    }

    private <T> void addRule(Predicate<T> predicate, Action<T> action) {
        interceptionRules.add(new InterceptionRule<>(predicate, action, false));
    }

    private <T> void addRule(Predicate<T> predicate, Action<T> action, boolean retainRule) {
        interceptionRules.add(new InterceptionRule<>(predicate, action, retainRule));
    }
}
