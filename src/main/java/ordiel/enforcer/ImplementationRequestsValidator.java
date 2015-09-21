package ordiel.enforcer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic.Kind;

import ordiel.enforcer.Types.*;
import ordiel.enforcer.Types.Void;

import org.kohsuke.MetaInfServices;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.Pair;

@MetaInfServices
@SupportedAnnotationTypes({ "ordiel.enforcer.TypeRequester",
		"ordiel.enforcer.MethodRequester",
		"ordiel.enforcer.RequestedTypes",
		"ordiel.enforcer.RequestedMethods" })
public class ImplementationRequestsValidator extends AbstractProcessor {

	// TODO: throw an error with Type requester is it is not Implemented in an
	// abstract class
	// TODO: check if its parents is abstract
	// TODO: check for inner classes and avoid collisions

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		TypeRequestedData trdHolder;
		MethodRequestedData mrdHolder;
		for (TypeElement evaluatedEncounteredAnnotation : annotations) {
			for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(
					evaluatedEncounteredAnnotation)) {
				TypeElement annotationEnclosingType = findEnclosingTypeElement(
						annotatedElement);
				try {
					if (evaluatedEncounteredAnnotation.getSimpleName()
							.toString().equals("TypeRequester")) {
						trdHolder = new TypeRequestedData(
								annotatedElement.getAnnotation(
										TypeRequester.class),
								annotationEnclosingType);
						if (!annotationEnclosingType.getModifiers().contains(
								Modifier.ABSTRACT)) {
							trdHolder.validateImplementation();
						}
					} else if (evaluatedEncounteredAnnotation.getSimpleName()
							.toString().equals("MethodRequester")) {
						mrdHolder = new MethodRequestedData(
								annotatedElement.getAnnotation(
										MethodRequester.class),
								annotationEnclosingType);
						if (!annotationEnclosingType.getModifiers().contains(
								Modifier.ABSTRACT)) {
							mrdHolder.validateImplementation();
						}
					} else if (evaluatedEncounteredAnnotation.getSimpleName()
							.toString().equals("RequestedTypes")) {
						TypeRequester[] requestedTypes = annotatedElement
								.getAnnotation(RequestedTypes.class).value();
						for (TypeRequester tr : requestedTypes) {
							trdHolder = new TypeRequestedData(
									tr, annotationEnclosingType);
							if (!annotationEnclosingType.getModifiers()
									.contains(Modifier.ABSTRACT)) {
								trdHolder.validateImplementation();
							}
						}
					} else if (evaluatedEncounteredAnnotation.getSimpleName()
							.toString().equals("RequestedMethods")) {
						MethodRequester[] requestedMethods = annotatedElement
								.getAnnotation(RequestedMethods.class).value();
						for (MethodRequester mr : requestedMethods) {
							mrdHolder = new MethodRequestedData(
									mr, annotationEnclosingType);
							if (!annotationEnclosingType.getModifiers()
									.contains(Modifier.ABSTRACT)) {
								mrdHolder.validateImplementation();
							}
						}
					}
				} catch (ClassNotFoundException cnfe) {
					cnfe.printStackTrace();
				}
			}
		}
		return true;
	}

	/**
	 * Using the provided {@link String} {@code modifierCompleteName} returns
	 * the {@link Modifier} for those values which ends with the appropriate
	 * name of an identifier. This method was created to facilitate the
	 * transformation of the objects returned when getting the value of a
	 * {@link Class}} type within an annotation during compilation time since
	 * this are of the type {@link com.sun.tools.javac.code.Attribute$Enum}.
	 * 
	 * @param modifierCompleateName
	 * 
	 * @return A {@link Modifier} that matches with the ending of named passed
	 *         as parameter.
	 */
	private Modifier getModifierForName(String modifierCompleateName) {
		// TODO: probably make this better, (switch)
		if (modifierCompleateName.endsWith("ABSTRACT")) {
			return Modifier.ABSTRACT;
		} else if (modifierCompleateName.endsWith("DEFAULT")) {
			return Modifier.DEFAULT;
		} else if (modifierCompleateName.endsWith("FINAL")) {
			return Modifier.FINAL;
		} else if (modifierCompleateName.endsWith("NATIVE")) {
			return Modifier.NATIVE;
		} else if (modifierCompleateName.endsWith("PRIVATE")) {
			return Modifier.PRIVATE;
		} else if (modifierCompleateName.endsWith("PROTECTED")) {
			return Modifier.PROTECTED;
		} else if (modifierCompleateName.endsWith("PUBLIC")) {
			return Modifier.PUBLIC;
		} else if (modifierCompleateName.endsWith("STATIC")) {
			return Modifier.STATIC;
		} else if (modifierCompleateName.endsWith("STRICTFP")) {
			return Modifier.STRICTFP;
		} else if (modifierCompleateName.endsWith("SYNCHRONIZED")) {
			return Modifier.SYNCHRONIZED;
		} else if (modifierCompleateName.endsWith("TRANSIENT")) {
			return Modifier.TRANSIENT;
		} else if (modifierCompleateName.endsWith("VOLATILE")) {
			return Modifier.VOLATILE;
		} else {
			// THEORETICALLY UNREACHABLE CODE
			throw new InputMismatchException(
					"The string passed as parameter does not match with any Modifier");
		}
	}

	public static TypeElement findEnclosingTypeElement(Element e) {
		while (e != null && !(e instanceof TypeElement)) {
			e = e.getEnclosingElement();
		}
		return TypeElement.class.cast(e);
	}

	private static String getRequestedType(Element annotatedElement,
			List<? extends AnnotationMirror> allElementAnnotationMirrors,
			String identifier) {
		String identifierFound,
				requestedType;
		for (AnnotationMirror am : allElementAnnotationMirrors) {
			if (am.getAnnotationType().toString().equals(
					"ordiel.enforcer.TypeRequester")) {
				identifierFound = null;
				requestedType = null;
				for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am
						.getElementValues().entrySet()) {
					if ("type()".equals(entry.getKey().toString())) {
						requestedType = entry.getValue().toString();
						requestedType = requestedType.substring(0, requestedType
								.length() - 6); // Removes the trailing '.class'
					}
					if ("identifier()".equals(entry.getKey().toString())) {
						if (!entry.getValue().toString().equals('"' + identifier
								+ '"')) {
							break; // THIS ANNOTATION MIRROR BELONGS TO ANOTHER
									// TYPE REQUESTER
						} else {
							identifierFound = entry.getValue().toString();
						}
					}
					if (requestedType != null && identifierFound != null) {
						return requestedType.toString();
					}
				}
			} else if (am.getAnnotationType().toString().equals(
					"ordiel.enforcer.RequestedTypes")) {
				for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am
						.getElementValues().entrySet()) {
					identifierFound = null;
					requestedType = null;
					for (com.sun.tools.javac.code.Attribute.Compound compoundAttribute : (List<com.sun.tools.javac.code.Attribute.Compound>) entry
							.getValue().getValue()) {
						for (Pair<MethodSymbol, Attribute> attributeData : compoundAttribute.values) {
							if ("type()".equals(attributeData.fst.toString())) {
								requestedType = attributeData.snd.toString();
								requestedType = requestedType.substring(0,
										requestedType.length() - 6); // Removes
																		// the
																		// trailing
																		// '.class'
							}
							if ("identifier()".equals(attributeData.fst
									.toString())) {
								if (!attributeData.snd.toString().equals('"'
										+ identifier + '"')) {
									break; // THIS ANNOTATION MIRROR BELONGS TO
											// ANOTHER TYPE REQUESTER
								} else {
									identifierFound = attributeData.snd
											.toString();
								}
							}
							if (requestedType != null
									&& identifierFound != null) {
								return requestedType.toString();
							}
						}
					}
				}
			} else {
				System.out.println("SABE: " + am.getAnnotationType());
			}
		}
		throw new RuntimeException(
				"Something happened!!!..." + " pedi: " + identifier);
	}

	private static String getReturnType(Element annotatedElement,
			List<? extends AnnotationMirror> allElementAnnotationMirrors,
			String identifier) {
		String identifierFound,
				requestedType;
		for (AnnotationMirror am : allElementAnnotationMirrors) {
			if (am.getAnnotationType().toString().equals(
					"ordiel.enforcer.RequestedMethods")) {
				for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am
						.getElementValues().entrySet()) {
					for (Compound compoundAttr : (com.sun.tools.javac.util.List<Compound>) entry
							.getValue().getValue()) {
						identifierFound = null;
						requestedType = null;
						for (Pair<MethodSymbol, Attribute> attributeData : compoundAttr.values) {
							if ("type()".equals(attributeData.fst.toString())) {
								requestedType = attributeData.snd.toString();
								requestedType = requestedType.substring(0,
										requestedType.length() - 6); // Removes
																		// the
																		// trailing
																		// '.class'
							}
							if ("identifier()".equals(attributeData.fst
									.toString())) {
								if (!attributeData.snd.toString().equals('"'
										+ identifier + '"')) {
									break; // THIS ANNOTATION MIRROR BELONGS TO
											// ANOTHER METHOD REQUESTER
								}
								identifierFound = attributeData.snd.toString();
							}
							if (requestedType != null
									&& identifierFound != null) {
								return requestedType.toString();
							}
						}
						if (identifierFound != null) {
							System.out.println(
									"THE CORRECT IDENTIFIER WAS FOUND BUT ITS TYPE IS NOT SATISFIED");
							System.out.println(identifierFound);
							break;
						}
					}
				}
			} else if (am.getAnnotationType().toString().equals(
					"ordiel.enforcer.MethodRequester")) {
				identifierFound = null;
				requestedType = null;
				for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am
						.getElementValues().entrySet()) {
					if ("type()".equals(entry.getKey().toString())) {
						requestedType = entry.getValue().toString().toString();
						requestedType = requestedType.substring(0, requestedType
								.length() - 6); // Removes the trailing '.class'
					}
					if ("identifier()".equals(entry.getKey().toString())) {
						if (!entry.getValue().toString().equals('"' + identifier
								+ '"')) {
							break; // THIS ANNOTATION MIRROR BELONGS TO ANOTHER
									// METHOD REQUESTER
							// TODO YOU CAN SEND AN ERROR HERE SINCE THIS SHOULD
							// BE THE ONLY ONE
						}
						identifierFound = entry.getValue().toString();
					}
					if (requestedType != null && identifierFound != null) {
						return requestedType.toString();
					}
				}
			}
		}
		throw new RuntimeException(
				"A return type was expected for the field " + identifier
						+ " inside " + annotatedElement + "...");
	}

	public static Class<?> getDataTypeByName(String dataTypeName)
			throws ClassNotFoundException {
		switch (dataTypeName) {
		case "byte":
			return PrimitiveByte.class;
		case "short":
			return PrimitiveShort.class;
		case "int":
			return PrimitiveInt.class;
		case "long":
			return PrimitiveLong.class;
		case "float":
			return PrimitiveFloat.class;
		case "double":
			return PrimitiveDouble.class;
		case "boolean":
			return PrimitiveBoolean.class;
		case "char":
			return PrimitiveChar.class;
		case "void":
		case "ordiel.enforcer.Types.Void":
			return Void.class;
		default:
			return Class.forName(dataTypeName);
		}
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		/*
		 * Overriding this method allows to avoid the warning of supported code
		 * version, the other way is specifying it explicitly:
		 * 
		 * import javax.annotation.processing.SupportedSourceVersion;
		 * 
		 * @SupportedSourceVersion(SourceVersion.RELEASE_8)
		 */
		return SourceVersion.latestSupported();
	}

	private abstract class RequestedData {
		private final String identifier;
		private final Class<?> requestedType;
		private final List<Modifier> requestedModifiers;
		private TypeElement parentContainerElement;
		Messager messager;

		private RequestedData(
				String identifier,
				Class<?> requestedType,
				Modifier[] modifiers,
				TypeElement parentContainerElement) {
			this.identifier = identifier;
			this.requestedType = requestedType;
			// Needed as mutable arraylist (java.util.ArrayList not
			// java.util.arrays.ArrayList)
			this.requestedModifiers = new ArrayList<Modifier>(
					Arrays.asList(modifiers));
			this.parentContainerElement = parentContainerElement;
			messager = ImplementationRequestsValidator.this.processingEnv
					.getMessager();
		}

		protected void validateImplementation() {
			boolean found = false;
			for (Element enclosedElement : parentContainerElement
					.getEnclosedElements()) {
				if (enclosedElement.getSimpleName().toString().equals(
						identifier)) {
					if (enclosedElement.getKind().equals(validateOver())) {
						found = true;
						if (enclosedElement.asType().toString().equals(
								requestedType.getCanonicalName()) ||
								(enclosedElement.asType().toString().equals(
										"byte") && requestedType
												.getCanonicalName().equals(
														PrimitiveByte.class
																.getCanonicalName()))
								||
								(enclosedElement.asType().toString().equals(
										"short") && requestedType
												.getCanonicalName().equals(
														PrimitiveShort.class
																.getCanonicalName()))
								||
								(enclosedElement.asType().toString().equals(
										"int") && requestedType
												.getCanonicalName().equals(
														PrimitiveInt.class
																.getCanonicalName()))
								||
								(enclosedElement.asType().toString().equals(
										"long") && requestedType
												.getCanonicalName().equals(
														PrimitiveLong.class
																.getCanonicalName()))
								||
								(enclosedElement.asType().toString().equals(
										"float") && requestedType
												.getCanonicalName().equals(
														PrimitiveFloat.class
																.getCanonicalName()))
								||
								(enclosedElement.asType().toString().equals(
										"double") && requestedType
												.getCanonicalName().equals(
														PrimitiveDouble.class
																.getCanonicalName()))
								||
								(enclosedElement.asType().toString().equals(
										"boolean") && requestedType
												.getCanonicalName().equals(
														PrimitiveBoolean.class
																.getCanonicalName()))
								||
								(enclosedElement.asType().toString().equals(
										"char") && requestedType
												.getCanonicalName().equals(
														PrimitiveChar.class
																.getCanonicalName()))
								||
								(enclosedElement.asType().toString().equals(
										"()void") && requestedType
												.getCanonicalName().equals(
														Void.class
																.getCanonicalName()))) {
							List<Modifier> modifiersFound = new ArrayList<Modifier>();
							modifiersFound.addAll(enclosedElement
									.getModifiers());
							for (int i = 0; i < requestedModifiers
									.size(); i++) {
								Modifier m = requestedModifiers.get(i);
								if (!modifiersFound.contains(m)) {
									messager.printMessage(Kind.ERROR,
											"The modifier '" + m
													+ "' was requested for the field '"
													+
													identifier
													+ "', but it was not found!!!...",
											enclosedElement);
								} else {
									modifiersFound.remove(m);
								}
								while (requestedModifiers.contains(m))
									requestedModifiers.remove(m);
							}
							if (modifiersFound.size() > 0) {
								messager.printMessage(Kind.WARNING,
										"One or more modifiers that were not requested were found for the field '"
												+
												identifier
												+ "'...\n\tModifiers: "
												+ modifiersFound,
										enclosedElement);
							}
						} else {
							messager.printMessage(Kind.ERROR,
									"The " + validateOver().toString()
											.toLowerCase() + " '" + identifier
											+ "' was requested to be of the type '"
											+ requestedType +
											"' but instead it is of the type '"
											+ enclosedElement.asType()
													.toString() + "'!!!...",
									enclosedElement);
						}
						break;
					}
				}
			}
			if (!found) {
				// TODO describe parameters of methods
				messager.printMessage(Kind.ERROR,
						"Incompleate implementation!!!...\nA " + validateOver()
								.toString().toLowerCase() + " with the next "
								+ "specifications is required on order to have a "
								+ "proper implementation:"
								+ "\n\tType:" + requestedType
								+ "\n\tIdentifier:" + identifier
								+ "\n\tModifiers: " + requestedModifiers + "",
						parentContainerElement);
			}
		}

		protected abstract ElementKind validateOver();

	}

	private class TypeRequestedData extends RequestedData {

		private TypeRequestedData(
				TypeRequester annotationToValidate,
				TypeElement parentContainerElement)
						throws ClassNotFoundException {
			super(annotationToValidate.identifier(),
					/*
					 * Line bellow - it is required to access by its string name
					 * since the class that it contains is not loaded in
					 * compilation time.
					 */
					getDataTypeByName(getRequestedType(parentContainerElement,
							processingEnv.getElementUtils()
									.getAllAnnotationMirrors(
											parentContainerElement),
							annotationToValidate.identifier())),
					annotationToValidate.requestedModifiers(),
					parentContainerElement);
		}

		@Override
		protected ElementKind validateOver() {
			return ElementKind.FIELD;
		}

	}

	private class MethodRequestedData extends RequestedData {
		private final List<Class<?>> parametersTypes;
		private final List<String> parametersIdentifiers;
		private MethodRequester annotationToValidate;
		private TypeElement parentContainerElement;

		private MethodRequestedData(
				MethodRequester annotationToValidate,
				TypeElement parentContainerElement)
						throws ClassNotFoundException {
			super(annotationToValidate.identifier(),
					/*
					 * Line bellow - it is required to access by its string name
					 * since the class that it contains is not loaded in
					 * compilation time.
					 */
					getDataTypeByName(getReturnType(parentContainerElement,
							processingEnv.getElementUtils()
									.getAllAnnotationMirrors(
											parentContainerElement),
							annotationToValidate.identifier())),
					annotationToValidate.requestedModifiers(),
					parentContainerElement);
			this.annotationToValidate = annotationToValidate;
			this.parentContainerElement = parentContainerElement;
			parametersIdentifiers = Arrays.asList(annotationToValidate
					.parametersIdentifiers());
			parametersTypes = getParameterTypes();
		}

		private List<Class<?>> getParameterTypesFromSimpleAnnotation() {
			return null;
		}

		private List<Class<?>> getParameterTypes()
				throws ClassNotFoundException {
			List<Class<?>> parameterTypes = null;
			String identifierFound,
					requestedType;
			Element annotationElement = null;
			for (AnnotationMirror am : processingEnv.getElementUtils()
					.getAllAnnotationMirrors(parentContainerElement)) {
				if (am.getAnnotationType().toString().equals(
						"ordiel.enforcer.RequestedMethods")) {
					for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am
							.getElementValues().entrySet()) {
						requestedType = identifierFound = null;
						parameterTypes = null;
						for (Compound compoundAttr : (com.sun.tools.javac.util.List<Compound>) entry
								.getValue().getValue()) {
							for (Pair<MethodSymbol, Attribute> pair : compoundAttr.values) {
								for (Compound cmpndAttr : (com.sun.tools.javac.util.List<Compound>) entry
										.getValue().getValue()) {
									for (Pair<MethodSymbol, Attribute> attributeData : cmpndAttr.values) {
										if ("identifier()".equals(
												attributeData.fst.toString())) {
											if (!(identifierFound = '"'
													+ annotationToValidate
															.identifier() + '"')
																	.equals(attributeData.snd
																			.toString())) {
												break; // The identifier is not
														// the expected this
														// annotation mirror
														// belongs to another
														// annotation
											}
											annotationElement = entry.getKey();
										}
										if ("parametersTypes()".equals(
												attributeData.fst.toString())) {
											parameterTypes = new ArrayList<Class<?>>();
											for (com.sun.tools.javac.code.Attribute.Class parameterType : (List<com.sun.tools.javac.code.Attribute.Class>) attributeData.snd
													.getValue()) {
												requestedType = parameterType
														.toString();
												requestedType = requestedType
														.substring(0,
																requestedType
																		.length()
																		- 6); // Removes
																				// the
																				// trailing
																				// '.class'
												parameterTypes.add(
														getDataTypeByName(
																requestedType));
											}
										}
									}
									if (parameterTypes != null
											&& identifierFound != null) {
										if (parametersIdentifiers
												.size() != parameterTypes
														.size()) {
											messager.printMessage(Kind.ERROR,
													"In the method element "
															+ identifierFound
															+ " posses an unequal number of parameter types and parameter identifiers",
													annotationElement);
										}
										return parameterTypes;
									}
								}
							}
						}
					}
				} else if (am.getAnnotationType().toString().equals(
						"ordiel.enforcer.MethodRequester")) {
					identifierFound = null;
					parameterTypes = null;
					for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am
							.getElementValues().entrySet()) {
						if ("identifier()".equals(entry.getKey().toString())) {
							if (!(identifierFound = annotationToValidate
									.identifier()).equals(entry.getValue()
											.getValue().toString())) {
								break; // The identifier is not the expected
										// this annotation mirror belongs to
										// another annotation
							}
							annotationElement = entry.getKey();
						}
						if ("parametersTypes()".equals(entry.getKey()
								.toString())) {
							parameterTypes = new ArrayList<Class<?>>();
							for (com.sun.tools.javac.code.Attribute.Class parameterType : (List<com.sun.tools.javac.code.Attribute.Class>) entry
									.getValue().getValue()) {
								requestedType = parameterType.toString();
								requestedType = requestedType.substring(0,
										requestedType.length() - 6); // Removes
																		// the
																		// trailing
																		// '.class'
								parameterTypes.add(getDataTypeByName(
										requestedType));
							}
						}
					}
					if (parameterTypes != null && identifierFound != null) {
						if (parametersIdentifiers.size() != parameterTypes
								.size()) {
							messager.printMessage(Kind.ERROR,
									"In the method element " + identifierFound
											+ " posses an unequal number of parameter types and parameter identifiers",
									annotationElement);
						}
						return parameterTypes;
					}
				} else {
					System.out.println("SABE: " + am.getAnnotationType());
				}
			}
			throw new RuntimeException(
					"Something happened!!!..."); // THROWN when no parameters
													// are found (TODO: check
													// wich other cases cause
													// this and fix it
		}

		@Override
		protected ElementKind validateOver() {
			return ElementKind.METHOD;
		}

		@Override
		protected void validateImplementation() {
			/*
			 * TODO Validate that the number of arguments for the parameters and
			 * the number of arguments for the class of the parameters are the
			 * same, and validate that this are implemented
			 */
			super.validateImplementation();
		}

	}

}
