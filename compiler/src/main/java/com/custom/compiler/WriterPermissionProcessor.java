package com.custom.compiler;

import com.custom.annotation.PermissionFail;
import com.custom.annotation.PermissionSuccess;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * Created by: Ysw on 2020/2/18.
 */
public class WriterPermissionProcessor implements IProcessor {
    @Override
    public void process(RoundEnvironment roundEnv, AnnotationProcessor annotationProcessor) {
        Set<? extends Element> SuccessAnnotatedWith = roundEnv.getElementsAnnotatedWith(PermissionSuccess.class);
        Set<? extends Element> FailAnnotatedWith = roundEnv.getElementsAnnotatedWith(PermissionFail.class);
        Map<String, List<ExecutableElement>> successMap = new HashMap<>();
        Map<String, List<ExecutableElement>> FailMap = new HashMap<>();
        for (Element element : SuccessAnnotatedWith) {
            ExecutableElement executableElement = (ExecutableElement) element;
            String activityName = executableElement.getEnclosingElement().getSimpleName().toString();
            List<ExecutableElement> elementList = successMap.get(activityName);
            if (elementList == null) {
                elementList = new ArrayList<>();
                successMap.put(activityName, elementList);
            }
            elementList.add(executableElement);
        }
        for (Element element : FailAnnotatedWith) {
            ExecutableElement executableElement = (ExecutableElement) element;
            String activityName = executableElement.getEnclosingElement().getSimpleName().toString();
            List<ExecutableElement> elementList = FailMap.get(activityName);
            if (elementList == null) {
                elementList = new ArrayList<>();
                FailMap.put(activityName, elementList);
            }
            elementList.add(executableElement);
        }
        if (successMap.size() > 0) {
            Writer writer = null;
            for (String activityName : successMap.keySet()) {
                List<ExecutableElement> elementList = successMap.get(activityName);
                TypeElement typeElement = (TypeElement) elementList.get(0).getEnclosingElement();
                String packageName = annotationProcessor.mProcessingEnv.getElementUtils().getPackageOf(typeElement).toString();
                try {
                    JavaFileObject sourceFile = annotationProcessor.filer.createSourceFile(packageName + "." + activityName + "_PermissionRequest");
                    writer = sourceFile.openWriter();
                    writer.write("package " + packageName + ";\n");
                    writer.write("import com.custom.annotation.PermissionRequest;\n");
                    writer.write("import androidx.annotation.NonNull;\n");
                    writer.write("import androidx.core.app.ActivityCompat;\n");
                    writer.write("import java.lang.ref.WeakReference;\n");
                    writer.write("final class " + activityName + "PermissionRequest{\n");
                    writer.write("private " + activityName + "PermissionRequest(){}\n");
                    for (int i = 0; i < elementList.size(); i++) {
                        String successMethodName = elementList.get(i).getSimpleName().toString();
                        writer.write("private static final int REQUEST_" + successMethodName.toUpperCase() + "=" + i + ";\n");
                        String[] value = elementList.get(i).getAnnotation(PermissionSuccess.class).value();
                        StringBuilder stringBuilder = new StringBuilder();
                        for (String s : value) {
                            stringBuilder.append('"').append(s).append('"' + ",");
                        }
                        String substring = stringBuilder.substring(0, stringBuilder.length() - 1);
                        writer.write("private static final String[] PERMISSION_" + successMethodName.toUpperCase() + " = " + "new String[]{" + substring + "};\n");
                        writer.write("public static void " + successMethodName + "PermissionCheck(@NonNull " + activityName + " target){\n");
                        writer.write("if (PermissionUtils.hasSelfPermissions(target,PERMISSION_" + successMethodName.toUpperCase() + ")){\n");
                        writer.write("target.requestContactsSuccess();\n");
                        writer.write("} else {\n");
                        writer.write("ActivityCompat.requestPermissions(target,PERMISSION_" + successMethodName.toUpperCase() + ",REQUEST_" + successMethodName.toUpperCase() + ");\n");
                        writer.write("}\n");
                        writer.write("}\n");
                    }
                    writer.write("static void onRequestPermissionResult(@NonNull " + activityName + " target,int requestCode, int[] grantResults){\n");
                    writer.write("switch (requestCode) {\n");
                    for (int i = 0; i < elementList.size(); i++) {
                        String successMethodName = elementList.get(i).getSimpleName().toString();
                        writer.write("case REQUEST_" + successMethodName.toUpperCase() + ":\n");
                        writer.write("if (PermissionUtils.verifyPermissions(grantResults)) {\n");
                        writer.write(" target." + successMethodName + "();\n");
                        writer.write("} else {\n");
                        writer.write("if (!PermissionUtils.shouldShowRequestPermissionRationale(target, PERMISSION_" + successMethodName.toUpperCase() + ")) {\n");
                        String[] successValue = elementList.get(i).getAnnotation(PermissionSuccess.class).value();
                        String failMethodName = "";
                        for (String name : FailMap.keySet()) {
                            if (!activityName.equals(name)) continue;
                            List<ExecutableElement> executableElements = FailMap.get(name);
                            for (ExecutableElement element : executableElements) {
                                String[] failValue = element.getAnnotation(PermissionFail.class).value();
                                if (!Arrays.toString(successValue).equals(Arrays.toString(failValue)))
                                    continue;
                                failMethodName = element.getSimpleName().toString();
                                break;
                            }
                        }
                        writer.write("target." + failMethodName + "(new " + activityName + successMethodName + "PermissionRequest(target),1);\n");
                        writer.write(" } else {\n");
                        writer.write("target." + failMethodName + "(new " + activityName + successMethodName + "PermissionRequest(target),0);\n");
                        writer.write("}\n}\n");
                        writer.write("break;\n");
                    }
                    writer.write("}\n}\n");
                    for (int i = 0; i < elementList.size(); i++) {
                        String successMethodName = elementList.get(i).getSimpleName().toString();
                        writer.write("private static final class " + activityName + successMethodName + "PermissionRequest implements PermissionRequest {\n");
                        writer.write("private final WeakReference<" + activityName + "> weakTarget;\n");
                        writer.write("private " + activityName + successMethodName + "PermissionRequest(@NonNull " + activityName + " target){\n");
                        writer.write("this.weakTarget = new WeakReference<>(target);\n");
                        writer.write("}\n");
                        writer.write("@Override\n");
                        writer.write("public void proceed() {\n");
                        writer.write(activityName + " target = weakTarget.get();\n");
                        writer.write("if (target == null) return;\n");
                        writer.write("ActivityCompat.requestPermissions(target,PERMISSION_" + successMethodName.toUpperCase() + ",REQUEST_" + successMethodName.toUpperCase() + ");\n");
                        writer.write("}\n");
                        writer.write("@Override\n");
                        writer.write("public void cancel() {\n");
                        writer.write(activityName + " target = weakTarget.get();\n");
                        writer.write("if (target == null) return;\n");
                        writer.write("}\n");
                        writer.write("}\n");
                    }
                    writer.write("}\n");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) {
                        try {
                            writer.flush();
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
