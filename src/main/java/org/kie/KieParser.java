package org.kie;

import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

public class KieParser extends Parser {

    public KieParser(ProblemReporter problemReporter, boolean optimizeStringLiterals) {
        super(problemReporter, optimizeStringLiterals);
    }

    @Override
    public void initializeScanner(){
        this.scanner = new KieScanner(
                false /*comment*/,
                false /*whitespace*/,
                false, /* will be set in initialize(boolean) */
                this.options.sourceLevel /*sourceLevel*/,
                this.options.complianceLevel /*complianceLevel*/,
                this.options.taskTags/*taskTags*/,
                this.options.taskPriorities/*taskPriorities*/,
                this.options.isTaskCaseSensitive/*taskCaseSensitive*/);
    }
}
