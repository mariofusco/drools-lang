package org.kie;

import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

public class KieCompiler extends org.eclipse.jdt.internal.compiler.Compiler {
    public KieCompiler(INameEnvironment environment,
                       IErrorHandlingPolicy policy,
                       CompilerOptions options,
                       ICompilerRequestor requestor,
                       IProblemFactory problemFactory) {
        super(environment, policy, options, requestor, problemFactory);
    }

    @Override
    public void initializeParser() {
        this.parser = new KieParser(this.problemReporter, this.options.parseLiteralExpressionsAsConstants);
    }
}
