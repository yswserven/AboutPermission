package com.custom.compiler;

import com.custom.annotation.PermissionFail;
import com.custom.annotation.PermissionSuccess;
import com.google.auto.service.AutoService;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {

    public Filer filer;
    public Elements elementUtils;
    public Messager messager;
    public ProcessingEnvironment mProcessingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        elementUtils = processingEnvironment.getElementUtils();
        messager = processingEnvironment.getMessager();
        this.mProcessingEnv = processingEnv;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(PermissionSuccess.class.getCanonicalName());
        types.add(PermissionFail.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        /* 通过 JavaPoet 实现代码生成 @author Ysw created 2020/2/18 */
        new PermissionProcessor().process(roundEnvironment, this);

        /* 直接通过 Writer 将代码写进 Java 文件中 @author Ysw created 2020/2/18 */
//        new WriterPermissionProcessor().process(roundEnvironment, this);
        return false;
    }
}
