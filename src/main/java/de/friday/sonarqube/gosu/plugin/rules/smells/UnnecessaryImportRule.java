/*
 * Copyright (C) 2023 FRIDAY Insurance S.A.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package de.friday.sonarqube.gosu.plugin.rules.smells;

import de.friday.sonarqube.gosu.antlr.GosuParser;
import de.friday.sonarqube.gosu.plugin.issues.GosuIssue;
import de.friday.sonarqube.gosu.plugin.rules.BaseGosuRule;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sonar.check.Rule;

import java.util.*;
import java.util.stream.Collectors;

@Rule(key = UnnecessaryImportRule.KEY)
public class UnnecessaryImportRule extends BaseGosuRule {

    static final String KEY = "UnnecessaryImportRule";

    private final Map<String, GosuParser.UsesStatementContext> allImports = new HashMap<>();
    private final Set<String> allReferencedClasses = new HashSet<>();
    private String currentPackage;
    private boolean afterUsesStatements = false;

    @Override
    protected String getKey() {
        return KEY;
    }

    @Override
    public void exitPackageDeclaration(GosuParser.PackageDeclarationContext context) {
        currentPackage = context.namespace().getText();
    }

    @Override
    public void exitUsesStatement(GosuParser.UsesStatementContext context) {
        if (isNamespaceAvailable(context)) {
            final String usesStatement = context.namespace().getText();
            checkUnnecessaryImport(usesStatement, context);
            String className = getClassName(usesStatement);
            allImports.putIfAbsent(className, context);
        }
    }

    @Override
    public void exitUsesFeatureLiteral(GosuParser.UsesFeatureLiteralContext context) {
        final List<String> staticImportClasses = context.children.stream()
                .map(ParseTree::getPayload)
                .filter(ParserRuleContext.class::isInstance)
                .map(parserRuleContext -> ((ParserRuleContext) parserRuleContext).getText())
                .collect(Collectors.toList());
        allReferencedClasses.addAll(staticImportClasses);
    }

    private boolean isNamespaceAvailable(GosuParser.UsesStatementContext context) {
        return context.namespace() != null;
    }

    @Override
    public void exitUsesStatementList(GosuParser.UsesStatementListContext ctx) {
        afterUsesStatements = true;
    }

    @Override
    public void exitIdentifier(GosuParser.IdentifierContext identifierContext) {
        final String identifier = identifierContext.getText();

        if (afterUsesStatements && identifier.matches("[A-Z].*")) {
            allReferencedClasses.add(identifier);
        }
    }

    @Override
    public void exitStart(GosuParser.StartContext ctx) {
        allImports.entrySet()
                .stream()
                .filter(elem -> !allReferencedClasses.contains(elem.getKey()))
                .forEach(elem -> addIssueWithMessage("There is unused import of " + elem.getKey() + ".",
                        elem.getValue()));
    }

    private void checkUnnecessaryImport(String usesStatement, GosuParser.UsesStatementContext ctx) {
        checkJavaLangImport(usesStatement, ctx);
        checkGwPersistedObjectsImport(usesStatement, ctx);
        checkSamePackageImport(usesStatement, ctx);
        checkDuplicateImport(usesStatement, ctx);
    }

    private void checkJavaLangImport(String usesStatement, GosuParser.UsesStatementContext ctx) {
        if (usesStatement.startsWith("java.lang.")) {
            addIssueWithMessage("Unnecessary import, java.lang classes are always available.", ctx);
        }
    }

    private void checkGwPersistedObjectsImport(String usesStatement, GosuParser.UsesStatementContext ctx) {
        if (usesStatement.startsWith("typekey.") || usesStatement.startsWith("entity.")) {
            addIssueWithMessage("Unnecessary import, typekey and entity classes are always available.", ctx);
        }
    }

    private void checkSamePackageImport(String usesStatement, GosuParser.UsesStatementContext ctx) {
        Objects.requireNonNull(currentPackage);

        if (usesStatement.equals(currentPackage)
                || (usesStatement.startsWith(currentPackage)
                && usesStatement.charAt(currentPackage.length()) == '.'
                && usesStatement.indexOf('.', currentPackage.length() + 1) == -1)) {
            addIssueWithMessage("Unnecessary import, same package classes are always available.", ctx);
        }
    }

    private void checkDuplicateImport(String usesStatement, GosuParser.UsesStatementContext ctx) {
        if (allImports.containsKey(getClassName(usesStatement))) {
            addIssueWithMessage("Unnecessary import, it is a duplicate.", ctx);
        }
    }

    private void addIssueWithMessage(String message, ParserRuleContext ctx) {
        addIssue(new GosuIssue.GosuIssueBuilder(this)
                .withMessage(message)
                .onContext(ctx)
                .build()
        );
    }

    String getClassName(String usesStatement) {
        int lastIndexOfDot = usesStatement.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            throw new IllegalArgumentException("No package found.");
        }

        return usesStatement.substring(lastIndexOfDot + 1);
    }

}
