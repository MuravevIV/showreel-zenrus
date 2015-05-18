package com.ilyamur.twitter.finatra

import java.io.{File, FileNotFoundException, FileOutputStream}
import java.lang.management.ManagementFactory
import java.net.SocketAddress

import com.ilyamur.netty.handler.WebsocketUpgrade
import com.twitter.conversions.storage._
import com.twitter.finagle._
import com.twitter.finagle.dispatch.SerialServerDispatcher
import com.twitter.finagle.http.{HttpMuxer, Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.finagle.netty3.{Netty3Listener, Netty3ListenerTLSConfig}
import com.twitter.finagle.server.DefaultServer
import com.twitter.finagle.ssl.Ssl
import com.twitter.finatra._
import com.twitter.util.Await
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

class FinatraServerWebosket extends FinatraTwitterServer {

    val controllers:  ControllerCollectionWebsocket = new ControllerCollectionWebsocket
    var filters:      Seq[Filter[FinagleRequest, FinagleResponse,FinagleRequest, FinagleResponse]] = Seq.empty
    val pid:          String = ManagementFactory.getRuntimeMXBean.getName.split('@').head

    var secureServer: Option[ListeningServer] = None
    var server:       Option[ListeningServer] = None
    var adminServer:  Option[ListeningServer] = None

    def allFilters(baseService: Service[FinagleRequest, FinagleResponse]):
    Service[FinagleRequest, FinagleResponse] = {
        filters.foldRight(baseService) { (b,a) => b andThen a }
    }

    def register(app: ControllerWebsocket) { controllers.add(app) }

    def addFilter(filter: Filter[FinagleRequest, FinagleResponse,FinagleRequest, FinagleResponse]) {
        filters = filters ++ Seq(filter)
    }

    def main() {
        log.info("finatra process " + pid + " started")
        start()
    }

    private[this] val nettyToFinagle =
        Filter.mk[HttpRequest, HttpResponse, FinagleRequest, FinagleResponse] { (req, service) =>
            service(FinagleRequest(req)) map { _.httpResponse }
        }


    private[this] lazy val service = {
        val appService  = new AppServiceWebsocket(controllers)
        val fileService = new FileService
        val loggingFilter = new LoggingFilter

        addFilter(loggingFilter)
        addFilter(fileService)

        nettyToFinagle andThen allFilters(appService)
    }

    private[this] lazy val codec = {
        val outerPipelineFactory = http.Http()
                .maxRequestSize(config.maxRequestSize().megabyte)
                .enableTracing(true)
                .server(ServerCodecConfig("httpserver", new SocketAddress{}))
                .pipelineFactory

        new ChannelPipelineFactory {
            def getPipeline = {
                val p = outerPipelineFactory.getPipeline
                p.addLast("websocketupgrade", new WebsocketUpgrade(controllers.controllers))
                p
            }
        }
    }

    def writePidFile() {
        val pidFile = new File(config.pidPath())
        val pidFileStream = new FileOutputStream(pidFile)
        pidFileStream.write(pid.getBytes)
        pidFileStream.close()
    }

    def removePidFile() {
        val pidFile = new File(config.pidPath())
        pidFile.delete()
    }

    def startSecureServer() {
        val tlsConfig =
            Some(Netty3ListenerTLSConfig(() => Ssl.server(config.certificatePath(), config.keyPath(), null, null, null)))
        object HttpsListener extends Netty3Listener[HttpResponse, HttpRequest]("https", codec, tlsConfig = tlsConfig)
        object HttpsServer extends DefaultServer[HttpRequest, HttpResponse, HttpResponse, HttpRequest](
            "https", HttpsListener, new SerialServerDispatcher(_, _)
        )
        log.info("https server started on port: " + config.sslPort())
        secureServer = Some(HttpsServer.serve(config.sslPort(), service))
    }

    def startHttpServer() {
        object HttpListener extends Netty3Listener[HttpResponse, HttpRequest]("http", codec)
        object HttpServer extends DefaultServer[HttpRequest, HttpResponse, HttpResponse, HttpRequest](
            "http", HttpListener, new SerialServerDispatcher(_, _)
        )
        log.info("http server started on port: " + config.port())
        server = Some(HttpServer.serve(config.port(), service))
    }

    def startAdminServer() {
        log.info("admin http server started on port: " + config.adminPort())
        adminServer = Some(HttpServer.serve(config.adminPort(), HttpMuxer))
    }

    def stop() {
        server map { _.close() }
        secureServer map { _.close() }
        adminServer map { _.close() }
    }

    onExit {
        stop()
        removePidFile()
    }

    def start() {

        if (!config.pidPath().isEmpty) {
            writePidFile()
        }

        if (!config.port().isEmpty) {
            startHttpServer()
        }

        if (!config.adminPort().isEmpty) {
            startAdminServer()
        }

        if (!config.certificatePath().isEmpty && !config.keyPath().isEmpty) {
            if (!new File(config.certificatePath()).canRead){
                val e = new FileNotFoundException("SSL Certificate not found: " + config.certificatePath())
                log.fatal(e, "SSL Certificate could not be read: " + config.certificatePath())
                throw e
            }
            if (!new File(config.keyPath()).canRead){
                val e = new FileNotFoundException("SSL Key not found: " + config.keyPath())
                log.fatal(e, "SSL Key could not be read: " + config.keyPath())
                throw e
            }
            startSecureServer()
        }

        server       map { Await.ready(_) }
        adminServer  map { Await.ready(_) }
        secureServer map { Await.ready(_) }

    }
}


