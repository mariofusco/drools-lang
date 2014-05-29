package org.eclipse.jdt;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.kie.lang.listener.Java7AntlrListener;
import org.kie.lang.parser.Java7Lexer;
import org.kie.lang.parser.Java7Parser;

public class Java7AntlrTest {
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

    @Test
    public void testJava7Parsing() {
        Java7Lexer lexer = new Java7Lexer(new ANTLRInputStream(person_java));

        // Get a list of matched tokens
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Pass the tokens to the parser
        Java7Parser parser = new Java7Parser(tokens);

        // Walk it and attach our listener
        ParseTreeWalker walker = new ParseTreeWalker();
        Java7AntlrListener listener = new Java7AntlrListener();
        walker.walk(listener, parser.compilationUnit());
    }
}
