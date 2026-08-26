package uk.sume.streamfolio.data.network

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Fakes [Interceptor.Chain] instead of hitting a real socket (e.g. via MockWebServer): the
 * interceptor's branching depends only on DNS/IP-literal resolution of `chain.request().url`,
 * so a canned in-memory [Response] is enough and keeps the test fully deterministic with no
 * network I/O for either the "blocked" or "allowed" cases.
 */
private class FakeChain(private val req: Request) : Interceptor.Chain {
    var proceeded = false
        private set

    override fun request(): Request = req

    override fun proceed(request: Request): Response {
        proceeded = true
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()
    }

    override fun connection(): Connection? = null
    override fun call(): Call = throw UnsupportedOperationException("not needed for this test")
    override fun connectTimeoutMillis(): Int = 0
    override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    override fun readTimeoutMillis(): Int = 0
    override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    override fun writeTimeoutMillis(): Int = 0
    override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
}

class SsrfProtectionInterceptorTest {

    private val interceptor = SsrfProtectionInterceptor()

    private fun requestTo(host: String) = Request.Builder().url("http://$host/feed.xml").build()

    @Test
    fun `blocks loopback host`() {
        val chain = FakeChain(requestTo("127.0.0.1"))
        try {
            interceptor.intercept(chain)
            fail("expected IOException for loopback host")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty().contains("SSRF"))
        }
        assertFalse("proceed() must not be called once blocked", chain.proceeded)
    }

    @Test
    fun `blocks localhost hostname via loopback resolution`() {
        val chain = FakeChain(requestTo("localhost"))
        try {
            interceptor.intercept(chain)
            fail("expected IOException for localhost")
        } catch (e: IOException) {
            // expected: localhost resolves to a loopback address
        }
    }

    @Test
    fun `blocks RFC1918 private range literal`() {
        for (host in listOf("10.1.2.3", "172.20.0.5", "192.168.50.1")) {
            val chain = FakeChain(requestTo(host))
            try {
                interceptor.intercept(chain)
                fail("expected IOException for private host $host")
            } catch (e: IOException) {
                // expected
            }
        }
    }

    @Test
    fun `blocks link-local metadata address literal`() {
        val chain = FakeChain(requestTo("169.254.169.254"))
        try {
            interceptor.intercept(chain)
            fail("expected IOException for link-local metadata address")
        } catch (e: IOException) {
            // expected
        }
    }

    @Test
    fun `allows public IP literal through to proceed`() {
        val chain = FakeChain(requestTo("8.8.8.8"))
        val response = interceptor.intercept(chain)
        assertTrue(chain.proceeded)
        assertEquals(200, response.code)
    }
}
