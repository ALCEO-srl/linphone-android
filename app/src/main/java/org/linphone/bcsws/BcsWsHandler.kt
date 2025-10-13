package org.linphone.bcsws

import java.security.cert.X509Certificate
import javax.net.ssl.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.linphone.core.tools.Log
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

                return builder
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    private var bearerToken = ""
    private var user = ""
    private var domain = ""
    private var password = ""

    fun SetUserInfo(aUser: String, aDomain: String, aPassword: String) {
        user = aUser
        domain = aDomain
        password = aPassword
        bearerToken = ""
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://$server:$port/")
        .addConverterFactory(GsonConverterFactory.create())
        // .client(getUnsafeOkHttpClient().build())
        .client(OkHttpClient.Builder().build())
        .build()

    private val bcsWsService = retrofit.create(BcsWsService::class.java)

    suspend fun fetchUserConf(): UserConf {
        return withContext(Dispatchers.IO) {
            requestAuthToken()
            bcsWsService.getUserConf("Bearer $bearerToken", domain, domain, user)
        }
    }

    suspend fun fetchDirectory(filter: String = ""): DirectoryResponse {
        return withContext(Dispatchers.IO) {
            requestAuthToken()
            bcsWsService.getDirectory("Bearer $bearerToken", domain, domain, "10000", filter)
        }
    }

    suspend fun requestAuthToken() {
        // Controllo se il bearerToken è vuoto
        Log.i("requestAuthToken(): Current bearerToken [$bearerToken]")
        if (bearerToken.isNullOrEmpty()) {
            val credentials = Credentials.basic(user + "@" + domain, password)

            // Richiesta di un nuovo token di autenticazione
            val authResponse = bcsWsService.requestAuthToken(credentials, domain, domain)
            if (authResponse != null) {
                // Assegnazione del nuovo token
                bearerToken = authResponse.access_token
                Log.i("requestAuthToken(): New bearerToken [$bearerToken]")
            } else {
                Log.i("requestAuthToken(): Request failed")
                // Gestione del caso in cui l'authResponse è nullo
                throw IllegalStateException("Authentication failed: authResponse is null")
            }
        }
    }

    suspend fun fetchCallReport(limit: Int? = null, offset: Int? = null): CallReportResponse {
        return withContext(Dispatchers.IO) {
            requestAuthToken()
            bcsWsService.getCallReport("Bearer $bearerToken", domain, domain, user, limit, offset)
        }
    }

    suspend fun clearAllCallReportItems() {
        return withContext(Dispatchers.IO) {
            requestAuthToken()
            val response = bcsWsService.clearCallReport("Bearer $bearerToken", domain, domain, user)
            if (!response.isSuccessful) {
                Log.e("BcsWsHandler", "Failed to clear call report: ${response.code()} - ${response.errorBody()?.string()}")
                throw IllegalStateException("Failed to clear call report")
            }
        }
    }

    suspend fun addCallReportEntry(item: CallReportItem): CallReportItem {
        return withContext(Dispatchers.IO) {
            requestAuthToken()
            bcsWsService.addCallReportItem("Bearer $bearerToken", domain, domain, user, item)
        }
    }

    suspend fun fetchCallReportItem(itemId: String): CallReportItem {
        return withContext(Dispatchers.IO) {
            requestAuthToken()
            bcsWsService.getCallReportItem("Bearer $bearerToken", domain, domain, user, itemId)
        }
    }

    suspend fun deleteCallReportEntry(itemId: String) {
        return withContext(Dispatchers.IO) {
            requestAuthToken()
            val response = bcsWsService.deleteCallReportItem("Bearer $bearerToken", domain, domain, user, itemId)
            if (!response.isSuccessful) {
                Log.e("BcsWsHandler", "Failed to delete call report item: ${response.code()} - ${response.errorBody()?.string()}")
                throw IllegalStateException("Failed to delete call report item")
            }
        }
    }
}
