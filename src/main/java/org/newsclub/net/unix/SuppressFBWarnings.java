package org.newsclub.net.unix;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
@interface SuppressFBWarnings {
   String[] value() default {};
}
