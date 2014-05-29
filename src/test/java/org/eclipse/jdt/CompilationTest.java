package org.eclipse.jdt;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.kie.KieCompiler;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

public class CompilationTest {

    private final String person_java =
            "package org.eclipse.jdt;\n" +
            "\n" +
            "public class Person {\n" +
            "    private final String name;\n" +
            "\n" +
            "    public Person(String name) {\n" +
            "        this.name = name;\n" +
            "    }\n" +
            "\n" +
            "    public String getName() {\n" +
            "        return name;\n" +
            "    }\n" +
            "}";

    private final String rule =
            "rule myRule {\n" +
            "    System.out.println(\"42\");\n" +
            "}";

    @Test
    public void testCompilation() {
        KieCompilerRequestor requestor = new KieCompilerRequestor();

        Compiler compiler = new KieCompiler( new KieNameEnvironment(getClass().getClassLoader()),
                                             DefaultErrorHandlingPolicies.proceedWithAllProblems(),
                                             new CompilerOptions(),
                                             requestor,
                                             new DefaultProblemFactory(Locale.getDefault()) );

        ICompilationUnit compilationUnit = new KieCompilationUnit("org/eclipse/jdt/Person.java", person_java);

        compiler.compile(new ICompilationUnit[] { compilationUnit });

        byte[] bytecode = requestor.getClasses().get("org/eclipse/jdt/Person.class");
        System.out.println(Arrays.toString(bytecode));
    }

    @Test
    public void testASTGeneration() {
        KieCompiler compiler1 = new KieCompiler( new KieNameEnvironment(getClass().getClassLoader()),
                                                 DefaultErrorHandlingPolicies.proceedWithAllProblems(),
                                                 new CompilerOptions(),
                                                 null,
                                                 new DefaultProblemFactory(Locale.getDefault()) );

        ICompilationUnit compilationUnit = new KieCompilationUnit("org/eclipse/jdt/Person.java", person_java);
        CompilationUnitDeclaration[] astUnits = compiler1.generateASTs(new ICompilationUnit[] { compilationUnit });

        KieCompilerRequestor requestor = new KieCompilerRequestor();
        KieCompiler compiler2 = new KieCompiler( new KieNameEnvironment(getClass().getClassLoader()),
                                                 DefaultErrorHandlingPolicies.proceedWithAllProblems(),
                                                 new CompilerOptions(),
                                                 requestor,
                                                 new DefaultProblemFactory(Locale.getDefault()) );

        compiler2.compileASTs(astUnits);

        byte[] bytecode = requestor.getClasses().get("org/eclipse/jdt/Person.class");
        System.out.println(Arrays.toString(bytecode));
    }

    public static class KieNameEnvironment implements INameEnvironment {

        private final ClassLoader classLoader;

        public KieNameEnvironment(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public NameEnvironmentAnswer findType( final char[][] pCompoundTypeName ) {
            final StringBuilder result = new StringBuilder();
            for (int i = 0; i < pCompoundTypeName.length; i++) {
                if (i != 0) {
                    result.append('.');
                }
                result.append(pCompoundTypeName[i]);
            }

            return findType(result.toString());
        }

        public NameEnvironmentAnswer findType( final char[] pTypeName, final char[][] pPackageName ) {
            final StringBuilder result = new StringBuilder();
            for (int i = 0; i < pPackageName.length; i++) {
                result.append(pPackageName[i]);
                result.append('.');
            }

            result.append(pTypeName);
            return findType(result.toString());
        }

        private NameEnvironmentAnswer findType( final String pClazzName ) {

            final String resourceName = ClassUtils.convertClassToResourcePath(pClazzName);

            InputStream is = null;
            ByteArrayOutputStream baos = null;
            try {
                is = classLoader.getResourceAsStream(resourceName);
                if (is == null) {
                    return null;
                }

                if ( ClassUtils.isWindows() || ClassUtils.isOSX() ) {
                    // check it really is a class, this issue is due to windows case sensitivity issues for the class org.kie.Process and path org/droosl/process
                    try {
                        classLoader.loadClass( pClazzName );
                    } catch ( ClassNotFoundException e ) {
                        return null;
                    } catch ( NoClassDefFoundError e ) {
                        return null;
                    }
                }

                final byte[] buffer = new byte[8192];
                baos = new ByteArrayOutputStream(buffer.length);
                int count;
                while ((count = is.read(buffer, 0, buffer.length)) > 0) {
                    baos.write(buffer, 0, count);
                }
                baos.flush();
                return createNameEnvironmentAnswer(pClazzName, baos.toByteArray());
            } catch ( final IOException e ) {
                throw new RuntimeException( "could not read class",
                                            e );
            } catch ( final ClassFormatException e ) {
                throw new RuntimeException( "wrong class format",
                                            e );
            } finally {
                try {
                    if (baos != null ) {
                        baos.close();
                    }
                } catch ( final IOException oe ) {
                    throw new RuntimeException( "could not close output stream",
                                                oe );
                }
                try {
                    if ( is != null ) {
                        is.close();
                    }
                } catch ( final IOException ie ) {
                    throw new RuntimeException( "could not close input stream",
                                                ie );
                }
            }
        }

        private NameEnvironmentAnswer createNameEnvironmentAnswer(final String pClazzName, final byte[] clazzBytes) throws ClassFormatException {
            final char[] fileName = pClazzName.toCharArray();
            final ClassFileReader classFileReader = new ClassFileReader(clazzBytes, fileName, true);
            return new NameEnvironmentAnswer(classFileReader, null);
        }

        private boolean isPackage( final String pClazzName ) {
            InputStream is = null;
            try {
                is = classLoader.getResourceAsStream(ClassUtils.convertClassToResourcePath(pClazzName));

                if (is != null) {
                    if (ClassUtils.isWindows() || ClassUtils.isOSX()) {
                        // check it really is a class, this issue is due to windows case sensitivity issues for the class org.kie.Process and path org/droosl/process

                        try {
                            Class cls = classLoader.loadClass(pClazzName);
                            if (cls != null) {
                                return true;
                            }
                        } catch (ClassNotFoundException e) {
                            return true;
                        } catch (NoClassDefFoundError e) {
                            return true;
                        }
                    }
                }
                return is == null;
            } finally {
                if ( is != null ) {
                    try {
                        is.close();
                    } catch ( IOException e ) {
                        throw new RuntimeException( "Unable to close stream for resource: " + pClazzName );
                    }
                }
            }
        }

        public boolean isPackage( char[][] parentPackageName, char[] pPackageName ) {
            final StringBuilder result = new StringBuilder();
            if (parentPackageName != null) {
                for (int i = 0; i < parentPackageName.length; i++) {
                    if (i != 0) {
                        result.append('.');
                    }
                    result.append(parentPackageName[i]);
                }
            }

            if (parentPackageName != null && parentPackageName.length > 0) {
                result.append('.');
            }
            result.append(pPackageName);
            return isPackage(result.toString());
        }

        public void cleanup() {
        }
    }

    public static class KieCompilerRequestor implements ICompilerRequestor {

        private final Collection problems = new ArrayList();
        private final Map<String, byte[]> classes = new HashMap<String, byte[]>();

        @Override
        public void acceptResult(CompilationResult result) {
            if (result.hasProblems()) {
                final IProblem[] iproblems = result.getProblems();
                for (int i = 0; i < iproblems.length; i++) {
                    final IProblem iproblem = iproblems[i];
                    problems.add(iproblem);
                }
            }
            if (!result.hasErrors()) {
                final ClassFile[] clazzFiles = result.getClassFiles();
                for (int i = 0; i < clazzFiles.length; i++) {
                    final ClassFile clazzFile = clazzFiles[i];
                    final char[][] compoundName = clazzFile.getCompoundName();
                    final StringBuilder clazzName = new StringBuilder();
                    for (int j = 0; j < compoundName.length; j++) {
                        if (j != 0) {
                            clazzName.append('.');
                        }
                        clazzName.append(compoundName[j]);
                    }
                    classes.put(clazzName.toString().replace('.', '/') + ".class", clazzFile.getBytes());
                }
            }
        }

        public Map<String, byte[]> getClasses() {
            return classes;
        }
    }

    public static class KieCompilationUnit implements ICompilationUnit {

        private final String sourceName;
        private final String source;

        private final String clazzName;
        private final char[] typeName;
        private final char[][] packageName;

        KieCompilationUnit( String sourceName, String source ) {
            clazzName = ClassUtils.convertResourceToClassName( sourceName );

            this.sourceName = sourceName;
            this.source = source;
            int dot = clazzName.lastIndexOf('.');
            if (dot > 0) {
                typeName = clazzName.substring(dot + 1).toCharArray();
            } else {
                typeName = clazzName.toCharArray();
            }

            final StringTokenizer izer = new StringTokenizer(clazzName, ".");
            packageName = new char[izer.countTokens() - 1][];
            for (int i = 0; i < packageName.length; i++) {
                packageName[i] = izer.nextToken().toCharArray();
            }
        }

        public char[] getFileName() {
            return sourceName.toCharArray();
        }

        public char[] getContents() {
            return source.toCharArray();
        }

        public char[] getMainTypeName() {
            return typeName;
        }

        public char[][] getPackageName() {
            return packageName;
        }

        public boolean ignoreOptionalProblems() {
            return true;
        }
    }
}
