// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class NextWindow extends AbstractTraverseWindowAction implements DumbAware {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    doPerform(w -> IdeEventQueue.getInstance().nextWindowAfter(w));
  }
}
