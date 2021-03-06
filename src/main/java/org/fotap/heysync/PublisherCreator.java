package org.fotap.heysync;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Method;
import java.util.List;

import static org.fotap.heysync.AsmHelper.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
class PublisherCreator<T> extends ClassCreator<T> {
    private final List<Method> methods;

    PublisherCreator(Class<T> type, Type outputType, List<Method> methods) {
        super(type, outputType);
        this.methods = methods;
    }

    @Override
    protected void createFields() {
        writer.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "SIGNAL", objectType.getDescriptor(), null, null)
                .visitEnd();

        for (Method method : methods) {
            createField(writer, method);
        }
    }

    private void createField(ClassWriter writer, Method method) {
        writer.visitField(ACC_PRIVATE + ACC_FINAL,
                fieldNameFor(method),
                publisherType.getDescriptor(),
                //Could put the generic signature... "Lorg/jetlang/channels/Publisher<Ljava/lang/String;>;",
                null,
                null)
                .visitEnd();
    }

    @Override
    protected void createConstructor() {
        constructorForPublishers();
        staticInitializer();
    }

    private void staticInitializer() {
        GeneratorAdapter adapter = method(ACC_STATIC, asmMethod("void <clinit> ()"));
        adapter.newInstance(objectType);
        adapter.dup();
        adapter.invokeConstructor(objectType, defaultConstructor);
        adapter.putStatic(outputType(), "SIGNAL", objectType);
        adapter.returnValue();
        adapter.endMethod();
    }

    private void constructorForPublishers() {
        GeneratorAdapter adapter = method(ACC_PUBLIC, asmConstructorMethod(methods.size()));
        adapter.loadThis();
        adapter.invokeConstructor(objectType, defaultConstructor);
        int arg = 0;

        for (Method method : methods) {
            adapter.loadThis();
            adapter.loadArg(arg++);
            adapter.putField(outputType(), fieldNameFor(method), publisherType);
        }

        adapter.returnValue();
        adapter.endMethod();
    }

    private org.objectweb.asm.commons.Method asmConstructorMethod(int parameters) {
        StringBuilder builder = new StringBuilder().append("void <init> (");

        for (int i = 0; i < parameters; i++) {
            builder.append(publisherType.getClassName()).append(",");
        }

        builder.deleteCharAt(builder.length() - 1).append(")");
        return org.objectweb.asm.commons.Method.getMethod(builder.toString());
    }

    @Override
    protected void implementMethods() {
        for (Method method : methods) {
            implement(method);
        }
    }

    private void implement(Method method) {
        GeneratorAdapter adapter = method(ACC_PUBLIC, asmMethod(method));
        adapter.loadThis();
        adapter.getField(outputType(), fieldNameFor(method), publisherType);
        loadMessage(method, adapter);
        adapter.invokeInterface(publisherType, publishMethod);
        adapter.returnValue();
        adapter.endMethod();
    }

    private void loadMessage(Method method, GeneratorAdapter adapter) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length > 1) {
            // Convert all method args into Object[] and push on to stack.
            adapter.loadArgArray();
        } else if (paramTypes.length == 1) {
            adapter.loadArg(0);
            Class<?> paramType = method.getParameterTypes()[0];
            if (paramType.isPrimitive()) {
                adapter.box(Type.getType(paramType));
            }
        } else {
            adapter.getStatic(outputType(), "SIGNAL", objectType);
        }
    }

    private String fieldNameFor(Method method) {
        // fails for multiple methods with the same name
        return method.getName() + "Publisher";
    }
}
