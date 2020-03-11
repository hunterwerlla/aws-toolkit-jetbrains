// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudwatch.logs

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.aws.toolkits.jetbrains.core.toolwindow.ToolkitToolWindowManager
import software.aws.toolkits.jetbrains.core.toolwindow.ToolkitToolWindowType
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor.CloudWatchLogGroup
import software.aws.toolkits.jetbrains.services.cloudwatch.logs.editor.CloudWatchLogStream
import software.aws.toolkits.jetbrains.utils.ApplicationThreadPoolScope
import software.aws.toolkits.jetbrains.utils.getCoroutineUiContext
import software.aws.toolkits.resources.message

class CloudWatchLogWindow(private val project: Project) : CoroutineScope by ApplicationThreadPoolScope("openLogGroup") {
    private val toolWindow = ToolkitToolWindowManager.getInstance(project, CW_LOGS_TOOL_WINDOW)
    private val edtContext = getCoroutineUiContext()

    fun showLogGroup(logGroup: String) = launch {
        val existingWindow = toolWindow.find(logGroup)
        if (existingWindow != null) {
            withContext(edtContext) {
                existingWindow.show()
            }
            return@launch
        }
        val group = CloudWatchLogGroup(project, logGroup)
        withContext(edtContext) {
            toolWindow.addTab(title = group.title, component = group.content, activate = true, id = logGroup, disposable = group)
        }
    }

    fun showLogStream(
        logGroup: String,
        logStream: String,
        fromHead: Boolean = true,
        startTime: Long? = null,
        timeScale: Long? = null
    ) = launch {
        val id = "$logGroup/$logStream"
        // dispose existing window if it exists to update. TODO add a refresh, duh
        val existingWindow = toolWindow.find(id)
        if (existingWindow != null) {
            withContext(edtContext) {
                existingWindow.dispose()
            }
        }
        val group = CloudWatchLogStream(project, logGroup, logStream, fromHead, startTime, timeScale)
        withContext(edtContext) {
            toolWindow.addTab(group.title, group.content, activate = true, id = id, disposable = group)
        }
    }

    companion object {
        private val CW_LOGS_TOOL_WINDOW = ToolkitToolWindowType("AWS.CloudWatchLog", message("cloudwatch.logs.toolwindow"))
        fun getInstance(project: Project) = ServiceManager.getService(project, CloudWatchLogWindow::class.java)
    }
}
