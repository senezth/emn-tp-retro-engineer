package tp.fil.main;

import java.beans.Visibility;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.BodyDeclaration;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.MethodDeclaration;
import org.eclipse.gmt.modisco.java.Modifier;
import org.eclipse.gmt.modisco.java.NamedElement;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.ParameterizedType;
import org.eclipse.gmt.modisco.java.SingleVariableDeclaration;
import org.eclipse.gmt.modisco.java.VisibilityKind;
import org.eclipse.gmt.modisco.java.TypeAccess;
import org.eclipse.gmt.modisco.java.VariableDeclarationFragment;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;

public class DataComputation {
	private static Resource javaModel;
	private static Resource dataModel;
	private static Resource dataMetamodel;
	
	public static void main(String[] args) {
		try {
			
			ResourceSet resSet = new ResourceSetImpl();
			resSet.getResourceFactoryRegistry().
				getExtensionToFactoryMap().
				put("ecore", new EcoreResourceFactoryImpl());
			resSet.getResourceFactoryRegistry().
				getExtensionToFactoryMap().
				put("xmi", new XMIResourceFactoryImpl());
			
			JavaPackage.eINSTANCE.eClass();
			
			dataMetamodel = resSet.createResource(URI.createFileURI("src/tp/fil/resources/Data.ecore"));
			dataMetamodel.load(null);
			EPackage.Registry.INSTANCE.put("http://data", dataMetamodel.getContents().get(0));
			
			javaModel = resSet.createResource(URI.createFileURI("../PetStore/PetStore_java.xmi"));
			javaModel.load(null);
			
			dataModel = resSet.createResource(URI.createFileURI("../PetStore/PetStore_data.xmi"));
			

			/*
			 * Beginning of the part to be completed...
			 */
			TreeIterator<EObject> iterator = javaModel.getAllContents();
			ArrayList<EObject> classes = new ArrayList<EObject>(); // save classes here
		
			while(iterator.hasNext()) {
				EObject currentElem = iterator.next();
				
				if(getName(currentElem).equals("ClassDeclaration")) {
					
					ClassDeclaration currentClass = (ClassDeclaration) currentElem;
					Package classPackage = currentClass.getPackage();
					
					if(classPackage != null && classPackage.getName().equals("model") && getAttribute(classPackage.getPackage(), "name").equals("petstore")) {
						
						String className = (String) getAttribute(currentClass, "name");
						Iterator<BodyDeclaration> bodyDeclarationIte = ((EList<BodyDeclaration>) getAttribute(currentClass, "bodyDeclarations")).iterator();
						ArrayList<EObject> attributes = new ArrayList<EObject>(); // save attributes here
						ArrayList<EObject> methods = new ArrayList<EObject>(); // save methods here
						
						while(bodyDeclarationIte.hasNext()) {
							
							BodyDeclaration currentBodyDeclaration = bodyDeclarationIte.next();
							
							if (getName(currentBodyDeclaration).equals("FieldDeclaration")) {
								EList<VariableDeclarationFragment> fragments = (EList<VariableDeclarationFragment>) getAttribute(currentBodyDeclaration, "fragments");
								VariableDeclarationFragment currentFragment = fragments.get(0);
								
								TypeAccess type = (TypeAccess) getAttribute(currentBodyDeclaration, "type");
								NamedElement typeType = (NamedElement) getAttribute(type, "type");
								
								Modifier modifier = (Modifier) getAttribute(currentBodyDeclaration, "modifier");
								VisibilityKind visibility = (VisibilityKind) getAttribute(modifier, "visibility");
								
								attributes.add(createAttribute(getAttribute(currentFragment, "name"), getAttribute(typeType, "name"), visibility.getName()));
							}
							else if (getName(currentBodyDeclaration).equals("MethodDeclaration")) {
//								MethodDeclaration currMethod = (MethodDeclaration) getAttribute(currentBodyDeclaration, attributeName)
								
								ArrayList<EObject> params = new ArrayList<EObject>();
								
								EList<VariableDeclarationFragment> fragments = (EList<VariableDeclarationFragment>) getAttribute(currentBodyDeclaration, "parameters");
								for (int i = 0; i < fragments.size(); i++) {
									SingleVariableDeclaration svd = (SingleVariableDeclaration) fragments.get(i);
									TypeAccess type = (TypeAccess) getAttribute(svd, "type");
									NamedElement typeType = (NamedElement) getAttribute(type, "type");
									params.add(createParameter(getAttribute(svd, "name"), getAttribute(typeType, "name")));
								}

								TypeAccess returntype = (TypeAccess) getAttribute(currentBodyDeclaration, "returnType");
								NamedElement returntypeType = (NamedElement) getAttribute(returntype, "type");

								Modifier modifier = (Modifier) getAttribute(currentBodyDeclaration, "modifier");
								VisibilityKind visibility = (VisibilityKind) getAttribute(modifier, "visibility");
								
								methods.add(createMethod(getAttribute(currentBodyDeclaration, "name"), getAttribute(returntypeType, "name"),  visibility.getName(), params));
							}
						}
						
						classes.add(createClass(className, attributes, methods)); // add class with attributes				
					}
				}
			}
			dataModel.getContents().add(createModel(classes)); // generate model with all classes and their attributes
			/*
			 * End of the part to be completed...
			 */
			
			dataModel.save(null);
			
			javaModel.unload();
			dataModel.unload();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static Object getAttribute(EObject elem, String attributeName) {
		return elem.eGet(elem.eClass().getEStructuralFeature(attributeName));
	}

	private static String getName(EObject elem) {
		return elem.eClass().getName();
	}

	private static EPackage getDataModelPackage(){
		EPackage totalPackage = (EPackage) dataMetamodel.getContents().get(0);
		return totalPackage;
	}

	private static EObject createModel(ArrayList<EObject> classes) {
		EPackage totalPackage = getDataModelPackage();
		EClass modelModel = (EClass) totalPackage.getEClassifier("Model");
		EObject modelObject = totalPackage.getEFactoryInstance().create(modelModel);
		modelObject.eSet(modelModel.getEStructuralFeature("class"), classes);
		return modelObject;
	}

	private static EObject createClass(String className, ArrayList<EObject> attributes, ArrayList<EObject> methods) {
		EPackage totalPackage = getDataModelPackage();
		EClass classModel = (EClass) totalPackage.getEClassifier("Class");
		EObject classObject = totalPackage.getEFactoryInstance().create(classModel);
		classObject.eSet(classModel.getEStructuralFeature("name"), className);
		classObject.eSet(classModel.getEStructuralFeature("attributes"), attributes);
		return classObject;
	}

	private static EObject createAttribute(Object name, Object type, Object visibility) {
		EPackage totalPackage = getDataModelPackage();
		EClass attributeModel = (EClass) totalPackage.getEClassifier("Attribute");
		EObject attributeObject = totalPackage.getEFactoryInstance().create(attributeModel);
		attributeObject.eSet(attributeModel.getEStructuralFeature("name"), name);
		attributeObject.eSet(attributeModel.getEStructuralFeature("type"), type);
		attributeObject.eSet(attributeModel.getEStructuralFeature("encapsulation"), visibility);
		return attributeObject;
	}
	
	private static EObject createMethod(Object name, Object returnn, Object visibility, ArrayList<EObject> Parameters) {
		EPackage totalPackage = getDataModelPackage();
		EClass methodModel = (EClass) totalPackage.getEClassifier("Method");
		EObject methodObject = totalPackage.getEFactoryInstance().create(methodModel);
		methodObject.eSet(methodModel.getEStructuralFeature("name"), name);
		methodObject.eSet(methodModel.getEStructuralFeature("return"), returnn);
		methodObject.eSet(methodModel.getEStructuralFeature("encapsulation"), visibility);
		methodObject.eSet(methodModel.getEStructuralFeature("Parameters"), Parameters);
		return methodObject;
	}
	
	private static EObject createParameter(Object name, Object type) {
		EPackage totalPackage = getDataModelPackage();
		EClass paramModel = (EClass) totalPackage.getEClassifier("Parameter");
		EObject paramObject = totalPackage.getEFactoryInstance().create(paramModel);
		paramObject.eSet(paramModel.getEStructuralFeature("name"), name);
		paramObject.eSet(paramModel.getEStructuralFeature("type"), type);
		return paramObject;
	}
}
