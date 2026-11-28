package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;

import java.util.List;
import java.util.Set;

public class CodeLineCounterTest extends BasePlatformTestCase {

    public void testCountsJavaCodeLines() {
        PsiFile file = myFixture.configureByText("A.java", """
                // header comment
                package p;

                /**
                 * Javadoc
                 */
                public class A { // trailing comment
                    /* block */ int x = 1;
                    String s = \"""
                        text
                        \""";
                }
                """);
        // package p; | public class A { | int x = 1; | String s = """ | text | """; | }
        assertEquals(7, CodeLineCounter.countCodeLines(file));
    }

    public void testCountsKotlinCodeLines() {
        PsiFile file = myFixture.configureByText("B.kt", "/** KDoc */\nfun f() {\n    // c\n    val x = 1\n}\n");
        assertEquals(3, CodeLineCounter.countCodeLines(file));
    }

    public void testEmptyFileIsZero() {
        PsiFile file = myFixture.configureByText("C.java", "// only comment\n\n");
        assertEquals(0, CodeLineCounter.countCodeLines(file));
    }

    private static final Set<Language> JAVA = Set.of(LanguageRegistry.PMD.getLanguageById("java"));

    public void testCountsOnlyFilesOfGivenLanguages() {
        PsiFile javaFile = myFixture.addFileToProject("D.java", "class D {\n  int x;\n}\n");
        PsiFile kotlinFile = myFixture.addFileToProject("E.kt", "fun f() {\n}\n");
        assertEquals(3, CodeLineCounter.countCodeLines(List.of(javaFile, kotlinFile), JAVA, new EmptyProgressIndicator()));
    }

    public void testSkipsInvalidFiles() {
        PsiFile javaFile = myFixture.addFileToProject("F.java", "class F {\n}\n");
        PsiFile deleted = myFixture.addFileToProject("G.java", "class G {\n}\n");
        WriteCommandAction.runWriteCommandAction(getProject(), deleted::delete);
        assertEquals(2, CodeLineCounter.countCodeLines(List.of(javaFile, deleted), JAVA, new EmptyProgressIndicator()));
    }

    public void testStopsWhenCancelled() {
        PsiFile javaFile = myFixture.addFileToProject("H.java", "class H {\n}\n");
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        indicator.cancel();
        assertThrows(ProcessCanceledException.class,
                () -> CodeLineCounter.countCodeLines(List.of(javaFile), JAVA, indicator));
    }
}
