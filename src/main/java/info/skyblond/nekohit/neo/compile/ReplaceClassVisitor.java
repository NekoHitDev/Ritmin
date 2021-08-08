package info.skyblond.nekohit.neo.compile;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM9;

public class ReplaceClassVisitor extends ClassVisitor {
    private final Map<String, String> replaceMap;
    public ReplaceClassVisitor(ClassVisitor cv, Map<String, String> replaceMap) {
        super(ASM9, cv);
        this.cv = cv;
        this.replaceMap = replaceMap;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        if (name.equals("<clinit>")) {
            // Here we replace the LDC instruction in clinit function
            //      where the static string literal is placed
            return new MethodVisitor(ASM9, mv) {
                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof String && replaceMap.containsKey(value)) {
                        value = replaceMap.get(value);
                    }
                    super.visitLdcInsn(value);
                }
            };
        }
        return mv;
    }
}
