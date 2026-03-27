package org.linphone.bcsws

import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.linphone.core.tools.Log
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// dms ******************

// Definizione dell'interfaccia Retrofit per le chiamate al webservice
interface BcsWsService {
    @FormUrlEncoded
    @POST("{clientid}/bcsws/v1/domains/{domain}/authtoken")
    suspend fun requestAuthToken(
        @Header("Authorization") authorization: String,
        @Path("clientid") cliendid: String,
        @Path("domain") domain: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): AuthResponse

    @GET("{clientid}/bcsws/v1/domains/{domain}/users/{user}")
    suspend fun getUserConf(
        @Header("Authorization") bearerToken: String,
        @Path("clientid") cliendid: String,
        @Path("domain") domain: String,
        @Path("user") user: String
    ): UserConf

    @GET("{clientid}/bcsws/v1/domains/{domain}/directory")
    suspend fun getDirectory(
        @Header("Authorization") bearerToken: String,
        @Path("clientid") cliendid: String,
        @Path("domain") domain: String,
        @Query("limit") limit: String,
        @Query("filter") filter: String
    ): DirectoryResponse

    @GET("{clientid}/bcsws/v1/domains/{domain}/users/{user}/callreport")
    suspend fun getCallReport(
        @Header("Authorization") bearerToken: String,
        @Path("clientid") cliendid: String,
        @Path("domain") domain: String,
        @Path("user") user: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): CallReportResponse

    @DELETE("{clientid}/bcsws/v1/domains/{domain}/users/{user}/callreport")
    suspend fun clearCallReport(
        @Header("Authorization") bearerToken: String,
        @Path("clientid") cliendid: String,
        @Path("domain") domain: String,
        @Path("user") user: String
    ): retrofit2.Response<Unit> // Usiamo Response<Unit> per una risposta senza body

    @POST("{clientid}/bcsws/v1/domains/{domain}/users/{user}/callreport")
    suspend fun addCallReportItem(
        @Header("Authorization") bearerToken: String,
        @Path("clientid") cliendid: String,
        @Path("domain") domain: String,
        @Path("user") user: String,
        @Body item: CallReportItem
    ): CallReportItem // Restituisce l'elemento con l'ID valorizzato

    @GET("{clientid}/bcsws/v1/domains/{domain}/users/{user}/callreport/{itemid}")
    suspend fun getCallReportItem(
        @Header("Authorization") bearerToken: String,
        @Path("clientid") cliendid: String,
        @Path("domain") domain: String,
        @Path("user") user: String,
        @Path("itemid") itemId: String
    ): CallReportItem

    @DELETE("{clientid}/bcsws/v1/domains/{domain}/users/{user}/callreport/{itemid}")
    suspend fun deleteCallReportItem(
        @Header("Authorization") bearerToken: String,
        @Path("clientid") cliendid: String,
        @Path("domain") domain: String,
        @Path("user") user: String,
        @Path("itemid") itemId: String
    ): retrofit2.Response<Unit>
}

class BcsWsHandler(server: String, port: String) {

    companion object {
        private const val TOKEN_EXPIRY_MARGIN_MS = 60_000L // rinnova il token 60s prima della scadenza

        fun getUnsafeOkHttpClient(): OkHttpClient.Builder {
            try {
                // Create a trust manager that does not validate certificate chains
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {}

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {}

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                })

                // Install the all-trusting trust manager
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())

                // Create an ssl socket factory with our all-trusting manager
                val sslSocketFactory = sslContext.socketFactory

                val builder = OkHttpClient.Builder()
                builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
                builder.connectTimeout(30, TimeUnit.SECONDS)
                builder.readTimeout(30, TimeUnit.SECONDS)
                builder.writeTimeout(30, TimeUnit.SECONDS)

                return builder
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    private val tokenMutex = Mutex()
    private var bearerToken = ""
    private var tokenExpiryTime = 0L // epoch millis
    private var user = ""
    private var domain = ""
    private var password = ""

    fun SetUserInfo(aUser: String, aDomain: String, aPassword: String) {
        user = aUser
        domain = aDomain
        password = aPassword
        bearerToken = ""
        tokenExpiryTime = 0L
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://$server:$port/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(getUnsafeOkHttpClient().build())
        .build()

    private val bcsWsService = retrofit.create(BcsWsService::class.java)

    // Garantisce un token valido e non scaduto; serializza eventuali refresh concorrenti.
    private suspend fun ensureValidToken() {
        tokenMutex.withLock {
            if (bearerToken.isEmpty() || System.currentTimeMillis() >= tokenExpiryTime) {
                Log.i("[BcsWsHandler] Token assente o scaduto, richiedo nuovo token")
                val credentials = Credentials.basic("$user@$domain", password)
                val authResponse = bcsWsService.requestAuthToken(credentials, domain, domain)
                bearerToken = authResponse.access_token
                tokenExpiryTime = System.currentTimeMillis() +
                    (authResponse.expires_in * 1000L) - TOKEN_EXPIRY_MARGIN_MS
                Log.i("[BcsWsHandler] Nuovo token acquisito, scade in ${authResponse.expires_in}s")
            }
        }
    }

    // Esegue la chiamata con il token corrente; in caso di 401 rinnova il token e riprova una volta.
    private suspend fun <T> withAuth(call: suspend (token: String) -> T): T {
        ensureValidToken()
        return try {
            call("Bearer $bearerToken")
        } catch (e: HttpException) {
            if (e.code() == 401) {
                Log.i("[BcsWsHandler] 401 ricevuto, rinnovo il token e riprovo")
                tokenMutex.withLock {
                    bearerToken = ""
                    tokenExpiryTime = 0L
                }
                ensureValidToken()
                call("Bearer $bearerToken")
            } else {
                throw e
            }
        }
    }

    suspend fun fetchUserConf(): UserConf {
        return withContext(Dispatchers.IO) {
            withAuth { token -> bcsWsService.getUserConf(token, domain, domain, user) }
        }
    }

    suspend fun fetchDirectory(filter: String = ""): DirectoryResponse {
        return withContext(Dispatchers.IO) {
            withAuth { token -> bcsWsService.getDirectory(token, domain, domain, "10000", filter) }
        }
    }

    suspend fun fetchCallReport(limit: Int? = null, offset: Int? = null): CallReportResponse {
        return withContext(Dispatchers.IO) {
            withAuth { token -> bcsWsService.getCallReport(token, domain, domain, user, limit, offset) }
        }
    }

    suspend fun clearAllCallReportItems() {
        return withContext(Dispatchers.IO) {
            val response = withAuth { token -> bcsWsService.clearCallReport(token, domain, domain, user) }
            if (!response.isSuccessful) {
                Log.e("[BcsWsHandler] clearAllCallReportItems failed: ${response.code()} - ${response.errorBody()?.string()}")
                throw IllegalStateException("Failed to clear call report")
            }
        }
    }

    suspend fun addCallReportEntry(item: CallReportItem): CallReportItem {
        return withContext(Dispatchers.IO) {
            withAuth { token -> bcsWsService.addCallReportItem(token, domain, domain, user, item) }
        }
    }

    suspend fun fetchCallReportItem(itemId: String): CallReportItem {
        return withContext(Dispatchers.IO) {
            withAuth { token -> bcsWsService.getCallReportItem(token, domain, domain, user, itemId) }
        }
    }

    suspend fun deleteCallReportEntry(itemId: String) {
        return withContext(Dispatchers.IO) {
            val response = withAuth { token -> bcsWsService.deleteCallReportItem(token, domain, domain, user, itemId) }
            if (!response.isSuccessful) {
                Log.e("[BcsWsHandler] deleteCallReportEntry($itemId) failed: ${response.code()} - ${response.errorBody()?.string()}")
                throw IllegalStateException("Failed to delete call report item")
            }
        }
    }
}
