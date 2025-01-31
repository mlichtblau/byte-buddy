package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.attribute.AnnotationRetention;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.test.packaging.EmptyType;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Test;

import java.lang.reflect.Field;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class RedefinitionDynamicTypeBuilderTest extends AbstractDynamicTypeBuilderForInliningTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String DEFAULT_METHOD_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    protected DynamicType.Builder<?> create(Class<?> type) {
        return new ByteBuddy().redefine(type);
    }

    protected DynamicType.Builder<?> createDisabledContext() {
        return new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE).redefine(Foo.class);
    }

    protected DynamicType.Builder<?> createDisabledRetention(Class<?> annotatedClass) {
        return new ByteBuddy().with(AnnotationRetention.DISABLED).redefine(annotatedClass);
    }

    protected DynamicType.Builder<?> createPlain() {
        return new ByteBuddy().redefine(Foo.class);
    }

    protected DynamicType.Builder<?> createPlainEmpty() {
        return new ByteBuddy().redefine(EmptyType.class);
    }

    protected DynamicType.Builder<?> create(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return new ByteBuddy().redefine(typeDescription, classFileLocator);
    }

    protected DynamicType.Builder<?> createPlainWithoutValidation() {
        return new ByteBuddy().with(TypeValidation.DISABLED).redefine(Foo.class);
    }

    @Test
    public void testConstructorRetentionNoAuxiliaryType() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .redefine(Bar.class)
                .make();
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(0));
        Class<?> type = dynamicType.load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
        Field field = type.getDeclaredField(BAR);
        assertThat(field.get(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) FOO));
    }

    @Test
    public void testConstructorRebaseSingleAuxiliaryType() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .redefine(Bar.class)
                .constructor(any()).intercept(MethodCall.invoke(Object.class.getDeclaredConstructor()))
                .make();
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(0));
        Class<?> type = dynamicType.load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
        Field field = type.getDeclaredField(BAR);
        assertThat(field.get(type.getDeclaredConstructor(String.class).newInstance(FOO)), nullValue(Object.class));
    }

    @Test
    public void testMethodRebase() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .redefine(Qux.class)
                .method(named(BAR)).intercept(StubMethod.INSTANCE)
                .make();
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(0));
        Class<?> type = dynamicType.load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(2));
        assertThat(type.getDeclaredMethod(FOO).invoke(null), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredMethod(BAR).invoke(null), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultInterfaceSubInterface() throws Exception {
        Class<?> interfaceType = Class.forName(DEFAULT_METHOD_INTERFACE);
        Class<?> dynamicInterfaceType = new ByteBuddy()
                .redefine(interfaceType)
                .method(named(FOO)).intercept(new Implementation.Simple(new TextConstant(BAR), MethodReturn.REFERENCE))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        Class<?> dynamicClassType = new ByteBuddy()
                .subclass(dynamicInterfaceType)
                .make()
                .load(dynamicInterfaceType.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicClassType.getMethod(FOO).invoke(dynamicClassType.getDeclaredConstructor().newInstance()), is((Object) BAR));
        assertThat(dynamicInterfaceType.getDeclaredMethods().length, is(1));
        assertThat(dynamicClassType.getDeclaredMethods().length, is(0));
    }

    public static class Bar {

        public final String bar;

        public Bar(String bar) {
            this.bar = bar;
        }
    }

    public static class Qux {

        public static String foo;

        public static String foo() {
            try {
                return foo;
            } finally {
                foo = FOO;
            }
        }

        public static String bar() {
            try {
                return foo;
            } finally {
                foo = FOO;
            }
        }
    }
}
