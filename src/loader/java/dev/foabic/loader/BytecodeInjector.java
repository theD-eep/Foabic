package dev.foabic.loader;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.List;
import java.util.Objects;

/**
 * Dependency-free method-entry/return injection built on the JDK's
 * {@code java.lang.classfile} API (Java 24+).
 *
 * <p>This is the whole "transformer" for Foabic: two instructions at a method
 * boundary, no ASM, no Mixin, no runtime bytecode library. Injections are
 * instance methods and call a static {@code (Object)void} hook with
 * {@code this} or a chosen local.
 */
public final class BytecodeInjector {

    private static final ClassDesc OBJECT = ClassDesc.of("java.lang.Object");
    private static final MethodTypeDesc HOOK_TYPE = MethodTypeDesc.of(ConstantDescs.CD_void, OBJECT);

    private static final ClassFile CLASS_FILE = ClassFile.of(
            ClassFile.StackMapsOption.GENERATE_STACK_MAPS,
            ClassFile.ShortJumpsOption.FIX_SHORT_JUMPS);

    private BytecodeInjector() {
    }

    /**
     * @param methodName the JVM method name to match
     * @param local      the local variable passed to the hook, usually 0 for
     *                   {@code this} or an argument slot
     * @param entryHook  static hook called at method entry, or null
     * @param returnHook static hook called before every return, or null
     */
    public record Injection(String methodName, int local, String entryHook, String returnHook) {
        public Injection {
            Objects.requireNonNull(methodName, "methodName");
            if (local < 0) {
                throw new IllegalArgumentException("local must be >= 0");
            }
            if (entryHook == null && returnHook == null) {
                throw new IllegalArgumentException("injection needs an entry or return hook");
            }
        }
    }

    public static byte[] inject(byte[] classBytes, ClassDesc hookOwner, List<Injection> injections) {
        Objects.requireNonNull(classBytes, "classBytes");
        Objects.requireNonNull(hookOwner, "hookOwner");
        Objects.requireNonNull(injections, "injections");

        ClassTransform transform = ClassTransform.ACCEPT_ALL;
        for (Injection injection : injections) {
            transform = transform.andThen(ClassTransform.transformingMethodBodies(
                    method -> !method.flags().has(AccessFlag.STATIC)
                            && method.methodName().stringValue().equals(injection.methodName()),
                    CodeTransform.ofStateful(() -> new InjectionTransform(hookOwner, injection))));
        }

        // Deliberately no ClassFile.verify() here. The default verifier may
        // resolve the class currently being defined, which recursively asks
        // the JVM to load the same class and produces a duplicate-definition
        // LinkageError. The JVM verifies the transformed bytes when defining
        // the class; unit tests additionally load and execute them.
        return CLASS_FILE.transformClass(CLASS_FILE.parse(classBytes), transform);
    }

    private static final class InjectionTransform implements CodeTransform {
        private final ClassDesc owner;
        private final Injection injection;
        private boolean entered;

        private InjectionTransform(ClassDesc owner, Injection injection) {
            this.owner = owner;
            this.injection = injection;
        }

        @Override
        public void accept(CodeBuilder builder, CodeElement element) {
            if (!entered && injection.entryHook() != null) {
                call(builder, injection.entryHook());
                entered = true;
            }
            if (injection.returnHook() != null && element instanceof ReturnInstruction) {
                call(builder, injection.returnHook());
            }
            builder.with(element);
        }

        private void call(CodeBuilder builder, String hookName) {
            builder.aload(injection.local());
            builder.invokestatic(owner, hookName, HOOK_TYPE);
        }
    }
}
