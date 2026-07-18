package uk.sume.streamfolio.data.network

import okhttp3.Interceptor
import okhttp3.Response
import uk.sume.streamfolio.util.UrlSecurityValidator
import java.io.IOException
import java.net.InetAddress

/**
 * OkHttp interceptor that blocks requests whose resolved IP address points to
 * private, loopback or link-local networks. This prevents DNS-rebinding based
 * SSRF attacks where an attacker-controlled hostname resolves to an internal
 * address at fetch time.
 */
class SsrfProtectionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host

        val address = runCatching { InetAddress.getByName(host) }.getOrNull()
        if (address != null && !UrlSecurityValidator.isResolvedAddressSafe(address)) {
            throw IOException("SSRF protection: request to $host resolves to a private/internal address")
        }

        return chain.proceed(request)
    }
}
