package org.kie.lang.listener;

import org.antlr.v4.runtime.misc.NotNull;
import org.kie.lang.parser.Java7BaseListener;
import org.kie.lang.parser.Java7Parser;

public class Java7AntlrListener extends Java7BaseListener {

    @Override
    public void enterClassDeclaration(@NotNull Java7Parser.ClassDeclarationContext ctx) {
        System.out.println(ctx.getText());
    }

    @Override
    public void exitClassDeclaration(@NotNull Java7Parser.ClassDeclarationContext ctx) {
        System.out.println(ctx.getText());
    }

    @Override
    public void enterPackageDeclaration(@NotNull Java7Parser.PackageDeclarationContext ctx) {
        System.out.println(ctx.getText());
    }

    @Override
    public void exitPackageDeclaration(@NotNull Java7Parser.PackageDeclarationContext ctx) {
        System.out.println(ctx.getText());
    }

    @Override
    public void enterQualifiedName(@NotNull Java7Parser.QualifiedNameContext ctx) {
        System.out.println(ctx.getText());
    }

    @Override
    public void exitQualifiedName(@NotNull Java7Parser.QualifiedNameContext ctx) {
        System.out.println(ctx.getText());
    }


}
