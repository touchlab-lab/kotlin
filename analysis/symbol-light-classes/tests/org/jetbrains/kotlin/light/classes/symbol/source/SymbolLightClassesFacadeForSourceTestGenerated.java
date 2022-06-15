/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.source;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link GenerateNewCompilerTests.kt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/asJava/ultraLightFacades")
@TestDataPath("$PROJECT_ROOT")
public class SymbolLightClassesFacadeForSourceTestGenerated extends AbstractSymbolLightClassesFacadeForSourceTest {
    @Test
    public void testAllFilesPresentInUltraLightFacades() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/asJava/ultraLightFacades"), Pattern.compile("^(.+)\\.(kt|kts)$"), null, true);
    }

    @Test
    @TestMetadata("coroutines.kt")
    public void testCoroutines() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/coroutines.kt");
    }

    @Test
    @TestMetadata("importAliases.kt")
    public void testImportAliases() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/importAliases.kt");
    }

    @Test
    @TestMetadata("inlineOnly.kt")
    public void testInlineOnly() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/inlineOnly.kt");
    }

    @Test
    @TestMetadata("jvmField.kt")
    public void testJvmField() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/jvmField.kt");
    }

    @Test
    @TestMetadata("jvmName.kt")
    public void testJvmName() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/jvmName.kt");
    }

    @Test
    @TestMetadata("jvmWildcardAnnotations.kt")
    public void testJvmWildcardAnnotations() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/jvmWildcardAnnotations.kt");
    }

    @Test
    @TestMetadata("lateinitProperty.kt")
    public void testLateinitProperty() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/lateinitProperty.kt");
    }

    @Test
    @TestMetadata("multifileFacade.kt")
    public void testMultifileFacade() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/multifileFacade.kt");
    }

    @Test
    @TestMetadata("multifileFacadeJvmName.kt")
    public void testMultifileFacadeJvmName() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/multifileFacadeJvmName.kt");
    }

    @Test
    @TestMetadata("properties.kt")
    public void testProperties() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/properties.kt");
    }

    @Test
    @TestMetadata("script.kts")
    public void testScript() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/script.kts");
    }

    @Test
    @TestMetadata("simpleFunctions.kt")
    public void testSimpleFunctions() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/simpleFunctions.kt");
    }

    @Test
    @TestMetadata("throwsAnnotation.kt")
    public void testThrowsAnnotation() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/throwsAnnotation.kt");
    }

    @Test
    @TestMetadata("wildcardOptimization.kt")
    public void testWildcardOptimization() throws Exception {
        runTest("compiler/testData/asJava/ultraLightFacades/wildcardOptimization.kt");
    }
}
