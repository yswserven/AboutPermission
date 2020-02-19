package com.custom.compiler;

import javax.annotation.processing.RoundEnvironment;

/**
 * Created by: Ysw on 2020/2/17.
 */
public interface IProcessor {
    void process(RoundEnvironment roundEnv, AnnotationProcessor annotationProcessor);
}
