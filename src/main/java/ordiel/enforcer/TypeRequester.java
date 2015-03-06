package ordiel.enforcer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.lang.model.element.Modifier;

import ordiel.enforcer.Types.UnvalidClass;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RequestedTypes.class)
@Inherited
public @interface TypeRequester {
	
	//TODO: Add array for forcing array types
	String identifier() default "";
	Class<?> type() default UnvalidClass.class;
	Modifier[] requestedModifiers() default {};
	
}
