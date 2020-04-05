// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.navigation.CtrlMouseInfo
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class GotoDeclarationOnlyAction : GotoDeclarationAction() {

  override fun getHandler(): CodeInsightActionHandler = GotoDeclarationOnlyHandler

  override fun getCtrlMouseInfo(editor: Editor, file: PsiFile, offset: Int): CtrlMouseInfo? {
    return null
  }
}
