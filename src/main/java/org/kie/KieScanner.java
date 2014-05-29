package org.kie;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;

public class KieScanner extends Scanner {

    public KieScanner(
            boolean tokenizeComments,
            boolean tokenizeWhiteSpace,
            boolean checkNonExternalizedStringLiterals,
            long sourceLevel,
            long complianceLevel,
            char[][] taskTags,
            char[][] taskPriorities,
            boolean isTaskCaseSensitive) {

        super(tokenizeComments,
              tokenizeWhiteSpace,
              checkNonExternalizedStringLiterals,
              sourceLevel,
              complianceLevel,
              taskTags,
              taskPriorities,
              isTaskCaseSensitive);
    }

    private static final char[] ruleV = "rule".toCharArray();

    @Override
    public int scanIdentifierOrKeyword() {
        int kind = super.scanIdentifierOrKeyword();
        if (kind != TerminalTokens.TokenNameIdentifier) return kind;

        char[] contents = getCurrentIdentifierSource();

        if (CharOperation.equals(ruleV, contents)) return TerminalTokens.TokenNameRule;

        return kind;
    }
}