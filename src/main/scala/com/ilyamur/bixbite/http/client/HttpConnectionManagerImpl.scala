package com.ilyamur.bixbite.http.client

import org.apache.http.conn.scheme.{PlainSocketFactory, SchemeRegistry, Scheme}
import javax.net.ssl.{TrustManagerFactory, TrustManager, SSLContext}
import java.security.{KeyStore, SecureRandom}
import org.apache.http.conn.ssl.SSLSocketFactory
import java.io.{File, FileInputStream}
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.auth.{UsernamePasswordCredentials, AuthScope}
import org.apache.http.conn.params.ConnRoutePNames
import org.apache.http.HttpHost

class HttpConnectionManagerImpl(optProxy: Option[Proxy],
                            optTruststore: Option[Truststore],
                            timeoutMs: Int) extends HttpConnectionManager {

    class HttpsSchemaProvider(truststore: Truststore) {

        def get: Scheme = {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, getTrustManagers(truststore), new SecureRandom)
            new Scheme("https", 443, new SSLSocketFactory(sslContext))
        }

        private def getTrustManagers(truststore: Truststore): Array[TrustManager] = {
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
            trustManagerFactory.init(getKeyStore(truststore))
            trustManagerFactory.getTrustManagers
        }

        private def getKeyStore(truststore: Truststore): KeyStore = {
            val keyStore = KeyStore.getInstance(truststore.kind)
            val fis = new FileInputStream(new File(truststore.filePath))
            try {
                keyStore.load(fis, truststore.password.toCharArray)
            } finally {
                try {
                    fis.close()
                } catch {
                    case ignored: Throwable =>
                }
            }
            keyStore
        }
    }

    //

    private lazy val connectionManager: ClientConnectionManager = {
        val schemeRegistry: SchemeRegistry = new SchemeRegistry
        registerHttpScheme(schemeRegistry)
        registerHttpsScheme(schemeRegistry)
        new ThreadSafeClientConnManager(schemeRegistry)
    }

    private def registerHttpScheme(schemeRegistry: SchemeRegistry) {
        schemeRegistry.register(new Scheme("http", 80, new PlainSocketFactory))
    }

    private def registerHttpsScheme(schemeRegistry: SchemeRegistry) {
        optTruststore.foreach { truststore =>
            val httpsSchemaProvider = new HttpsSchemaProvider(truststore)
            schemeRegistry.register(httpsSchemaProvider.get)
        }
    }

    def createHttpClient: DefaultHttpClient = {
        val httpClient = new DefaultHttpClient(connectionManager)
        setProxy(httpClient)
        setTimeout(httpClient)
        httpClient
    }

    private def setProxy(httpClient: DefaultHttpClient) {
        optProxy.foreach { proxy =>
            val authScope = new AuthScope(proxy.host, proxy.port)
            val credentials = new UsernamePasswordCredentials(proxy.login, proxy.password)
            httpClient.getCredentialsProvider.setCredentials(authScope, credentials)
            httpClient.getParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxy.host, proxy.port))
        }
    }

    private def setTimeout(httpClient: DefaultHttpClient) {
        httpClient.getParams.setParameter("http.socket.timeout", timeoutMs)
    }

    def close() {
        connectionManager.shutdown()
    }
}
