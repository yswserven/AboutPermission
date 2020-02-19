package com.custom.compiler;

import com.custom.annotation.PermissionFail;
import com.custom.annotation.PermissionRequest;
import com.custom.annotation.PermissionSuccess;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * Created by: Ysw on 2020/2/17.
 */
public class PermissionProcessor implements IProcessor {
    @Override
    public void process(RoundEnvironment roundEnv, AnnotationProcessor annotationProcessor) {
        Set<? extends Element> successAnnotatedWith = roundEnv.getElementsAnnotatedWith(PermissionSuccess.class);
        Set<? extends Element> failAnnotatedWith = roundEnv.getElementsAnnotatedWith(PermissionFail.class);
        Map<String, List<ExecutableElement>> successMap = new HashMap<>();
        Map<String, List<ExecutableElement>> failMap = new HashMap<>();
        for (Element element : successAnnotatedWith) {
            ExecutableElement executableElement = (ExecutableElement) element;
            String activityName = executableElement.getEnclosingElement().getSimpleName().toString();
            List<ExecutableElement> elementList = successMap.get(activityName);
            if (elementList == null) {
                elementList = new ArrayList<>();
                successMap.put(activityName, elementList);
            }
            elementList.add(executableElement);
        }
        for (Element element : failAnnotatedWith) {
            ExecutableElement executableElement = (ExecutableElement) element;
            String activityName = executableElement.getEnclosingElement().getSimpleName().toString();
            List<ExecutableElement> elementList = failMap.get(activityName);
            if (elementList == null) {
                elementList = new ArrayList<>();
                failMap.put(activityName, elementList);
            }
            elementList.add(executableElement);
        }
        if (successMap.size() > 0) {
            for (String activityName : successMap.keySet()) {
                List<ExecutableElement> elementList = successMap.get(activityName);
                TypeElement typeElement = (TypeElement) elementList.get(0).getEnclosingElement();
                String packageName = annotationProcessor.elementUtils.getPackageOf(typeElement).toString();
                TypeSpec.Builder builder = classBuilder(activityName + "_PermissionRequest").addModifiers(FINAL);
                MethodSpec constructorMethodSpec = MethodSpec.constructorBuilder().addModifiers(PRIVATE).build();
                builder.addMethod(constructorMethodSpec);
                builder.addMethod(createMethod(activityName, elementList, failMap.get(activityName)));
                for (int i = 0; i < elementList.size(); i++) {
                    String successMethodName = elementList.get(i).getSimpleName().toString();
                    FieldSpec requestFieldSpec = FieldSpec.builder(int.class, "REQUEST_" +
                            successMethodName.toUpperCase()).addModifiers(PRIVATE, STATIC, FINAL).initializer("$L", i).build();
                    builder.addField(requestFieldSpec);

                    String[] value = elementList.get(i).getAnnotation(PermissionSuccess.class).value();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String s : value) {
                        stringBuilder.append('"').append(s).append('"').append(",");
                    }
                    String substring = stringBuilder.substring(0, stringBuilder.length() - 1);
                    FieldSpec permissionFieldSpec = FieldSpec.builder(String[].class, "PERMISSION_" +
                            successMethodName.toUpperCase()).addModifiers(PRIVATE, STATIC, FINAL).initializer("new $T {$L}", String[].class, substring).build();
                    builder.addField(permissionFieldSpec);

                    MethodSpec permissionCheckMethodSpec = MethodSpec.methodBuilder(successMethodName + "PermissionCheck")
                            .addModifiers(PUBLIC, STATIC)
                            .returns(void.class)
                            .addParameter(ParameterSpec.builder(ClassName.get(typeElement), "target").build())
                            .addCode("if(PermissionUtils.hasSelfPermissions(target,PERMISSION_" + successMethodName.toUpperCase() + ")){\n")
                            .addCode("target." + successMethodName + "();\n")
                            .addCode("} else {\n")
                            .addCode("androidx.core.app.ActivityCompat.requestPermissions(target, PERMISSION_" + successMethodName.toUpperCase() + "," + i + ");\n")
                            .addCode("}\n").build();
                    builder.addMethod(permissionCheckMethodSpec);
                    builder.addType(createType(activityName, successMethodName, elementList.get(i), i));
                }
                try {
                    JavaFile.builder(packageName, builder.build()).build().writeTo(annotationProcessor.filer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private MethodSpec createMethod(String activityName, List<ExecutableElement> successElementList, List<ExecutableElement> failElementList) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("onRequestPermissionResult")
                .addModifiers(STATIC)
                .returns(void.class)
                .addParameter(ParameterSpec.builder(ClassName.get((TypeElement) successElementList.get(0).getEnclosingElement()), "target").build())
                .addParameter(int.class, "requestCode")
                .addParameter(int[].class, "grantResults");
        builder.addCode("switch(requestCode) {\n");
        for (int i = 0; i < successElementList.size(); i++) {
            String successMethodName = successElementList.get(i).getSimpleName().toString();
            String failMethodName = failElementList.get(i).getSimpleName().toString();
            builder.addCode("case $L:\n", i);
            builder.addCode("if (PermissionUtils.verifyPermissions(grantResults)) {\n");
            builder.addCode("target.$L();\n", successMethodName);
            builder.addCode("} else if (!PermissionUtils.shouldShowRequestPermissionRationale(target, PERMISSION_$L)) {\n", successMethodName.toUpperCase());
            builder.addCode("target.$L(new $L$LPermissionRequest(target),1);\n", failMethodName, activityName, successMethodName);
            builder.addCode("} else {");
            builder.addCode("target.$L(new $L$LPermissionRequest(target),0);\n", failMethodName, activityName, successMethodName);
            builder.addCode("}\n");
            builder.addCode("break;\n");
        }
        builder.addCode("}\n");
        return builder.build();
    }

    private TypeSpec createType(String activityName, String successMethodName, ExecutableElement executableElement, int i) {
        TypeSpec.Builder builder = classBuilder(activityName + successMethodName + "PermissionRequest")
                .addModifiers(PRIVATE, STATIC, FINAL)
                .addSuperinterface(PermissionRequest.class);
        FieldSpec weakTarget = FieldSpec.builder(WeakReference.class, "weakTarget").addModifiers(PRIVATE, FINAL).build();
        builder.addField(weakTarget);
        MethodSpec constructorMethod = MethodSpec.constructorBuilder().addModifiers(PRIVATE)
                .addParameter(ParameterSpec.builder(ClassName.get((TypeElement) executableElement.getEnclosingElement()), "target").build())
                .addStatement("this.weakTarget = new WeakReference(target)").build();
        builder.addMethod(constructorMethod);

        MethodSpec proceed = MethodSpec.methodBuilder("proceed").addModifiers(PUBLIC).returns(void.class)
                .addCode(ClassName.get((TypeElement) executableElement.getEnclosingElement()) + " target = (" + ClassName.get((TypeElement) executableElement.getEnclosingElement()) + ")this.weakTarget.get();\n")
                .addCode(" if (target != null) {\n")
                .addCode("androidx.core.app.ActivityCompat.requestPermissions(target," + activityName + "_PermissionRequest.PERMISSION_" + successMethodName.toUpperCase() + "," + i + ");\n}\n")
                .build();
        builder.addMethod(proceed);

        MethodSpec cancel = MethodSpec.methodBuilder("cancel").addModifiers(PUBLIC).returns(void.class)
                .addCode(ClassName.get((TypeElement) executableElement.getEnclosingElement()) + " target = (" + ClassName.get((TypeElement) executableElement.getEnclosingElement()) + ")this.weakTarget.get();\n")
                .addCode(" if (target != null) {\n")
                .addCode("}\n").build();
        builder.addMethod(cancel);
        return builder.build();
    }
}
