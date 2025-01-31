package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ImplementationContextDefaultOtherTest {

    private static final String FOO = "foo";

    @Test
    public void testFactory() throws Exception {
        assertThat(Implementation.Context.Default.Factory.INSTANCE.make(mock(TypeDescription.class),
                mock(AuxiliaryType.NamingStrategy.class),
                mock(TypeInitializer.class),
                mock(ClassFileVersion.class),
                mock(ClassFileVersion.class)), instanceOf(Implementation.Context.Default.class));
    }

    @Test
    public void testFactoryWithFixedSuffix() throws Exception {
        assertThat(new Implementation.Context.Default.Factory.WithFixedSuffix(FOO).make(mock(TypeDescription.class),
                mock(AuxiliaryType.NamingStrategy.class),
                mock(TypeInitializer.class),
                mock(ClassFileVersion.class),
                mock(ClassFileVersion.class)), instanceOf(Implementation.Context.Default.class));
    }

    @Test
    public void testEnabled() throws Exception {
        assertThat(new Implementation.Context.Default(mock(TypeDescription.class),
                mock(ClassFileVersion.class),
                mock(AuxiliaryType.NamingStrategy.class),
                mock(TypeInitializer.class),
                mock(ClassFileVersion.class),
                FOO).isEnabled(), is(true));
    }

    @Test
    public void testInstrumentationGetter() throws Exception {
        TypeDescription instrumentedType = mock(TypeDescription.class);
        assertThat(new Implementation.Context.Default(instrumentedType,
                mock(ClassFileVersion.class),
                mock(AuxiliaryType.NamingStrategy.class),
                mock(TypeInitializer.class),
                mock(ClassFileVersion.class),
                FOO).getInstrumentedType(), is(instrumentedType));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDefaultContext() throws Exception {
        new Implementation.Context.Default.DelegationRecord(mock(MethodDescription.InDefinedShape.class), Visibility.PACKAGE_PRIVATE) {
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                throw new AssertionError();
            }

            @Override
            protected Implementation.Context.Default.DelegationRecord with(MethodAccessorFactory.AccessType accessType) {
                throw new AssertionError();
            }
        }.prepend(mock(ByteCodeAppender.class));
    }
}
