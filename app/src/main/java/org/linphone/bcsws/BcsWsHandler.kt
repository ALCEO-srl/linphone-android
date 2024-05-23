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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    @POST("bcsws/v1/domains/{domain}/authtoken")
    suspend fun requestAuthToken(
        @Header("Authorization") authorization: String,
        @Path("domain") domain: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): AuthResponse

    @GET("bcsws/v1/domains/{domain}/users/{user}")
    suspend fun getUserConf(
        @Header("Authorization") bearerToken: String,
        @Path("domain") domain: String,
        @Path("user") user: String
    ): UserConf

    @GET("bcsws/v1/domains/{domain}/directory")
    suspend fun getDirectory(
        @Header("Authorization") bearerToken: String,
        @Path("domain") domain: String,
        @Query("limit") limit: String,
        @Query("filter") filter: String
    ): DirectoryResponse
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
        .client(getUnsafeOkHttpClient().build())
        .build()

    private val bcsWsService = retrofit.create(BcsWsService::class.java)

    suspend fun fetchUserConf(): UserConf {
        return withContext(Dispatchers.IO) {
            requestAuthToken()
            bcsWsService.getUserConf("Bearer $bearerToken", domain, user)
        }
    }

    suspend fun fetchDirectory(filter: String = ""): DirectoryResponse {
        return withContext(Dispatchers.IO) {
            requestAuthToken()
            bcsWsService.getDirectory("Bearer $bearerToken", domain, "10000", filter)
        }
    }

    suspend fun requestAuthToken() {
        // Controllo se il bearerToken è vuoto
        if (bearerToken.isEmpty()) {
            val credentials = Credentials.basic(user + "@" + domain, password)

            // Richiesta di un nuovo token di autenticazione
            val authResponse = bcsWsService.requestAuthToken(credentials, domain)
            if (authResponse != null) {
                // Assegnazione del nuovo token
                bearerToken = authResponse.access_token
            } else {
                // Gestione del caso in cui l'authResponse è nullo
                throw IllegalStateException("Authentication failed: authResponse is null")
            }
        }
    }
}
