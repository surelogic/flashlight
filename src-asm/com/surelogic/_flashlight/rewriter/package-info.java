/**
 * This package contains classes that perform the rewriting of classfiles to 
 * instrument them to obtain dynamic locking information.  Instrumented classes
 * are not allowed to depend on the existence of any class in this package. 
 * Instrumented classes may depend on classes in
 * {@link com.surelogic._flashlight.rewriter.runtime}.
 * 
 * <p>The main class in this package is {@link FlashlightClassRewriter}.
 */
package com.surelogic._flashlight.rewriter;
