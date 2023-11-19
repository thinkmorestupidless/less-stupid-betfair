package com.thinkmorestupidless.betfair.streams.impl

import com.thinkmorestupidless.betfair.core.impl.SocketConfig
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Flow, Tcp}
import org.apache.pekko.util.ByteString

import java.net.InetSocketAddress
import javax.net.ssl.{SSLContext, SSLEngine}

object TlsSocketFlow {

  type TlsSocketFlow = Flow[ByteString, ByteString, NotUsed]

  def fromConfig(config: SocketConfig)(implicit system: ActorSystem): TlsSocketFlow = {
    val address = new InetSocketAddress(config.uri.value, config.port.value)
    Tcp().outgoingConnectionWithTls(address, () => createSSLEngine()).mapMaterializedValue(_ => NotUsed)
  }

  private def createSSLEngine(): SSLEngine = {
    val engine = SSLContext.getDefault.createSSLEngine()

    engine.setUseClientMode(true)
    engine.setEnabledCipherSuites(Array("TLS_RSA_WITH_AES_128_CBC_SHA"))
    engine.setEnabledProtocols(Array("TLSv1.2"))

    engine
  }
}
