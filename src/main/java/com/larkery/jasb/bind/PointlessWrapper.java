package com.larkery.jasb.bind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Temporary help to indicate to the writer that this
 * class is a pointless wrapper for whatever property
 * it contains
 * 
 * @author hinton
 *
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PointlessWrapper {

}
