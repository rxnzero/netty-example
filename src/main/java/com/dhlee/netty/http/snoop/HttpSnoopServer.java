package com.dhlee.netty.http.snoop;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * An HTTP server that sends back the content of the received HTTP request
 * in a pretty plaintext form.
 */
public final class HttpSnoopServer {

    static final boolean SSL = true; //System.getProperty("ssl") != null;
    static final boolean SELF_SSL = true; 
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
        	
        	if(SELF_SSL) {
        		// Self 인증서
            	 SelfSignedCertificate ssc = new SelfSignedCertificate();
	            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        	}
        	else {
	        	// Java KeyStore 사용        	
	            File keyStoreFile = new File("C:\\Users\\elink\\.keystore");
	            String keyPassword = "changeit";
	            
	            String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
	            SSLContext serverContext = SSLContext.getInstance("TLSv1.2"); //JDK 7 버전부터 지원합니다.
	            final KeyStore ks = KeyStore.getInstance("JKS");
	            
	            ks.load(
	            		new FileInputStream(keyStoreFile), 
	            		keyPassword.toCharArray()
	            		);
	            
	            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
	            kmf.init(ks, keyPassword.toCharArray());
	            sslCtx = SslContextBuilder.forServer(kmf).build();
        	}
            
        } else {
            sslCtx = null;
        }

        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new HttpSnoopServerInitializer(sslCtx));

            Channel ch = b.bind(PORT).sync().channel();

            System.err.println("Open your web browser and navigate to " +
                    (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

