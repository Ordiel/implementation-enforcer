package ordiel.enforcer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.lang.model.element.Modifier;

import ordiel.enforcer.Types.Void;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RequestedMethods.class)
@Inherited
public @interface MethodRequester {
	
	//By default has to be static
	//if parametersIdentifiers are specified they must be of the same length of parameters 
	//TODO: Add array for forcing array types
	String identifier() default "";
	Class<?> type() default Void.class;
	Class<?>[] parametersTypes() default {};
	String[] parametersIdentifiers() default {};
	Modifier[] requestedModifiers() default {};
	
//	public static class Parameter{
//		final Class<?> parameterType;
//		final String identifier;
//		
//		public Parameter(Class<?> parameterType, String identifier) {
//			this.parameterType = parameterType;
//			this.identifier = identifier;
//		}
//		
//	}
	
}
