package com.github.thething.chipamp.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the visibility of a type, method, or field has been relaxed
 * (e.g. from {@code private} to package-private or {@code public}) solely to
 * make it accessible in tests.
 */
@Retention(value = RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface VisibleForTesting {
}
