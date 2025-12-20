package miroshka.aether.addons;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AddonInfo {

    String id();

    String name();

    String version();

    String author() default "Unknown";

    String description() default "";

    String[] dependencies() default {};

    String[] softDependencies() default {};
}
