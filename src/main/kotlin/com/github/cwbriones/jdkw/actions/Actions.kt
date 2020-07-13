package com.github.cwbriones.jdkw.actions

import com.github.cwbriones.jdkw.services.JdkWrapperService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class ImportJdk : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<JdkWrapperService>()?.onOpen()
//        StringBuffer dlgMsg = new StringBuffer(event.getPresentation().getText() + " Selected!");
//        String dlgTitle = event.getPresentation().getDescription();
//        // If an element is selected in the editor, add info about it.
//        Navigatable nav = event.getData(CommonDataKeys.NAVIGATABLE);
//        if (nav != null) {
//            dlgMsg.append(String.format("\nSelected Element: %s", nav.toString()));
//        }
//        Messages.showMessageDialog(currentProject, dlgMsg.toString(), dlgTitle, Messages.getInformationIcon());
    }
}