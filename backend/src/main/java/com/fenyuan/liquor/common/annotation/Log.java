package com.fenyuan.liquor.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    String value() default "";

    String module() default "";
}
