package eu.kanade.tachiyomi.extension

import java.net.URI

internal object AggregateCatalogPolicy {
    const val SEARCH_FILTER = "comic-aggregate"
    const val MAX_PACKAGE_BYTES = 20L * 1024 * 1024

    fun validateUrl(value: String): String {
        val uri = URI(value.trim())
        require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) { "请输入完整的 HTTP 或 HTTPS 图源地址" }
        require(uri.userInfo == null && uri.fragment == null) { "图源地址不能包含账号、密码或锚点" }
        return uri.toASCIIString()
    }

    fun checkSigningKey(previous: String, incoming: String) {
        require(incoming.matches(Regex("[a-fA-F0-9]{64}"))) { "订阅缺少有效签名身份" }
        require(previous.isBlank() || previous.equals(incoming, ignoreCase = true)) { "订阅签名身份发生变化，已停止自动更新" }
    }
}
