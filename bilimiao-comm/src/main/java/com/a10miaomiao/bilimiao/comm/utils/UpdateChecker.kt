//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package com.a10miaomiao.bilimiao.comm.utils

import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GiteeReleaseInfo(
    val id: Long,
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<GiteeAsset> = emptyList()
)

@Serializable
data class GiteeAsset(
    val name: String,
    val browser_download_url: String
)

sealed class UpdateCheckResult {
    data class Success(val version: String, val content: String, val url: String) : UpdateCheckResult()
    data object NoUpdate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateChecker {
    private const val GITEE_RELEASE_URL = "https://gitee.com/api/v5/repos/Voltula/as/releases/latest"

    suspend fun checkForUpdates(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val response = MiaoHttp.request(GITEE_RELEASE_URL).awaitCall()
            val body = response.body?.string() ?: throw Exception("Response body is null")
            val jsonParser = Json { ignoreUnknownKeys = true }
            val res = jsonParser.decodeFromString<GiteeReleaseInfo>(body)
            val tagName = res.tag_name
            val newVersion = tagName.replace(Regex("[^\\d.]"), "")
            val currentVersion = currentVersionName.replace(Regex("[^\\d.]"), "")

            if (compareVersions(newVersion, currentVersion) > 0) {
                val apkUrl = res.assets.firstOrNull { it.name.endsWith(".apk") }?.browser_download_url
                    ?: "https://gitee.com/Voltula/as/releases"
                UpdateCheckResult.Success(
                    version = newVersion,
                    content = res.body,
                    url = apkUrl
                )
            } else {
                UpdateCheckResult.NoUpdate
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateCheckResult.Error(e.message ?: e.toString())
        }
    }

    fun compareVersions(v1: String, v2: String): Int {
        val vals1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val vals2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(vals1.size, vals2.size)) {
            val num1 = vals1.getOrNull(i) ?: 0
            val num2 = vals2.getOrNull(i) ?: 0
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }
}