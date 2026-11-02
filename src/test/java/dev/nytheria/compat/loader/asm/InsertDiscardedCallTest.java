package dev.nytheria.compat.loader.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * {@link NytheriaCompatEarlyRiser#insertDiscardedCall} on a stand-in for LiquidBlock: a class with a {@code fluid} field, a
 * {@code getFluid()} method and methods that read the field, written with exactly the stack size they need, as javac
 * does. Plain unit tests, no game classes and no Fabric Loader.
 */
public class InsertDiscardedCallTest {
    private static final String OWNER = "dev/nytheria/compat/loader/asm/generated/Liquid";

    /** Counts the calls of getFluid(), so that the test can see that the inserted call runs. */
    public static int fluidRequests;

    private static ClassNode liquid() {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = OWNER;
        node.superName = "java/lang/Object";
        node.fields.add(new FieldNode(Opcodes.ACC_PROTECTED, "fluid", "Ljava/lang/Object;", null, null));
        node.fields.add(new FieldNode(Opcodes.ACC_PROTECTED, "ticks", "J", null, null));

        MethodNode init = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/Object;)V", null, null);
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitVarInsn(Opcodes.ALOAD, 1);
        init.visitFieldInsn(Opcodes.PUTFIELD, OWNER, "fluid", "Ljava/lang/Object;");
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(2, 2);
        node.methods.add(init);

        // Object getFluid() { fluidRequests++; return this.fluid; }
        MethodNode getFluid = new MethodNode(Opcodes.ACC_PUBLIC, "getFluid", "()Ljava/lang/Object;", null, null);
        getFluid.visitFieldInsn(Opcodes.GETSTATIC, "dev/nytheria/compat/loader/asm/InsertDiscardedCallTest", "fluidRequests", "I");
        getFluid.visitInsn(Opcodes.ICONST_1);
        getFluid.visitInsn(Opcodes.IADD);
        getFluid.visitFieldInsn(Opcodes.PUTSTATIC, "dev/nytheria/compat/loader/asm/InsertDiscardedCallTest", "fluidRequests", "I");
        getFluid.visitVarInsn(Opcodes.ALOAD, 0);
        getFluid.visitFieldInsn(Opcodes.GETFIELD, OWNER, "fluid", "Ljava/lang/Object;");
        getFluid.visitInsn(Opcodes.ARETURN);
        getFluid.visitMaxs(2, 1);
        node.methods.add(getFluid);

        // long getTicks() { return this.ticks; }
        MethodNode getTicks = new MethodNode(Opcodes.ACC_PUBLIC, "getTicks", "()J", null, null);
        getTicks.visitVarInsn(Opcodes.ALOAD, 0);
        getTicks.visitFieldInsn(Opcodes.GETFIELD, OWNER, "ticks", "J");
        getTicks.visitInsn(Opcodes.LRETURN);
        getTicks.visitMaxs(2, 1);
        node.methods.add(getTicks);

        // Object read(boolean really) { if (really) { return this.fluid; } return null; }
        // One stack slot, one branch target with a stack map frame: what a small LiquidBlock method looks like.
        MethodNode read = new MethodNode(Opcodes.ACC_PUBLIC, "read", "(Z)Ljava/lang/Object;", null, null);
        Label start = new Label();
        Label otherwise = new Label();
        Label end = new Label();
        read.visitLabel(start);
        read.visitVarInsn(Opcodes.ILOAD, 1);
        read.visitJumpInsn(Opcodes.IFEQ, otherwise);
        read.visitVarInsn(Opcodes.ALOAD, 0);
        read.visitFieldInsn(Opcodes.GETFIELD, OWNER, "fluid", "Ljava/lang/Object;");
        read.visitInsn(Opcodes.ARETURN);
        read.visitLabel(otherwise);
        read.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        read.visitInsn(Opcodes.ACONST_NULL);
        read.visitInsn(Opcodes.ARETURN);
        read.visitLabel(end);
        read.visitLocalVariable("this", "L" + OWNER + ";", null, start, end, 0);
        read.visitLocalVariable("really", "Z", null, start, end, 1);
        read.visitMaxs(1, 2);
        node.methods.add(read);

        return node;
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }

    /** What Mixin does before a local-capturing injection: it replays the method with the declared stack size. */
    private static Frame<BasicValue>[] analyze(MethodNode method) throws AnalyzerException {
        return new Analyzer<>(new BasicVerifier()).analyze(OWNER, method);
    }

    /** Writes the class as it is, without recomputing stack sizes or frames, and lets the JVM verify and load it. */
    private static Class<?> define(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();

        return new ClassLoader(InsertDiscardedCallTest.class.getClassLoader()) {
            Class<?> define() {
                return this.defineClass(OWNER.replace('/', '.'), bytes, 0, bytes.length);
            }
        }.define();
    }

    @Test
    public void theMethodStillAnalysesAndRuns() throws Exception {
        ClassNode node = liquid();
        MethodNode read = method(node, "read");
        AbstractInsnNode originalStart = read.instructions.getFirst();
        int originalSize = read.instructions.size();

        NytheriaCompatEarlyRiser.insertDiscardedCall(read, OWNER, "getFluid", "()Ljava/lang/Object;");

        assertEquals(originalSize + 3, read.instructions.size());
        assertEquals(1, read.maxStack);
        assertEquals(2, read.maxLocals);

        // The stack is empty again where the original code starts, and the analysis gets through the whole method.
        Frame<BasicValue>[] frames = analyze(read);
        assertEquals(0, frames[read.instructions.indexOf(originalStart)].getStackSize());

        for (AbstractInsnNode insn : read.instructions) {
            if (insn.getOpcode() >= 0) {
                assertTrue(frames[read.instructions.indexOf(insn)] != null, "an instruction became unreachable");
            }
        }

        // The JVM accepts the class with the stack size and the frame it had, and the inserted call really runs.
        Class<?> liquid = define(node);
        Object fluid = new Object();
        Object instance = liquid.getConstructor(Object.class).newInstance(fluid);
        fluidRequests = 0;

        assertSame(fluid, liquid.getMethod("read", boolean.class).invoke(instance, true));
        assertSame(null, liquid.getMethod("read", boolean.class).invoke(instance, false));
        assertEquals(2, fluidRequests);
    }

    @Test
    public void withoutThePopTheAnalysisFails() {
        // The insertion as it was: the result of getFluid() stays on the stack. This is the exception every start printed.
        MethodNode read = method(liquid(), "read");
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
        call.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, OWNER, "getFluid", "()Ljava/lang/Object;"));
        read.instructions.insert(call);

        AnalyzerException exception = assertThrows(AnalyzerException.class, () -> analyze(read));
        assertTrue(exception.getMessage().contains("Insufficient maximum stack size"), exception.getMessage());
    }

    @Test
    public void aWideResultIsDroppedWhole() throws Exception {
        ClassNode node = liquid();
        MethodNode read = method(node, "read");

        NytheriaCompatEarlyRiser.insertDiscardedCall(read, OWNER, "getTicks", "()J");

        // A long needs two slots while it is on the stack; the method only had one.
        assertEquals(2, read.maxStack);
        assertEquals(Opcodes.POP2, read.instructions.get(2).getOpcode());
        analyze(read);

        Class<?> liquid = define(node);
        Object fluid = new Object();
        Object instance = liquid.getConstructor(Object.class).newInstance(fluid);
        assertSame(fluid, liquid.getMethod("read", boolean.class).invoke(instance, true));
    }
}
