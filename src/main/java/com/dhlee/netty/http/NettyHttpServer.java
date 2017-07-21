package com.dhlee.netty.http;

import static io.netty.buffer.Unpooled.copiedBuffer;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

public class NettyHttpServer {
	private ChannelFuture channel;
	private final EventLoopGroup masterGroup;
	private final EventLoopGroup slaveGroup;
	
	String charsetName = "utf-8";
	
	public NettyHttpServer() {
		masterGroup = new NioEventLoopGroup();
		slaveGroup = new NioEventLoopGroup();
	}
	
	private String handleMessage(String request) {
		String response = 
				"<?xml version=\"1.0\" encoding=\"" + charsetName + "\" ?>" +
				"<response><message>응답 - Hello from Netty!<message><response>";
		System.out.println("# Body : " + request);	
		return response;
	}
	
	public void start(int port) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdown();
			}
		});

		try {
			final ServerBootstrap bootstrap = new ServerBootstrap().group(masterGroup, slaveGroup)
					.channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() 
					{
						@Override
						public void initChannel(final SocketChannel ch) throws Exception {
							ch.pipeline().addLast("codec", new HttpServerCodec());
							ch.pipeline().addLast("aggregator", new HttpObjectAggregator(512 * 1024));
							ch.pipeline().addLast("request", new ChannelInboundHandlerAdapter()
							{	
								@Override
								public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
									if (msg instanceof FullHttpRequest) {
										Charset charset = Charset.forName(charsetName);
										
										final FullHttpRequest request = (FullHttpRequest) msg;
										System.out.println("//--------------------------------------------------------------------------");
										System.out.println("Request : " + request.toString());
										System.out.println("URI : " + request.uri());
										
										//UTF_8  Charset.forName("euc-kr") , CharsetUtil.UTF_8;
//										QueryStringDecoder decoder = new QueryStringDecoder(request.uri(), CharsetUtil.UTF_8); 
										QueryStringDecoder decoder = new QueryStringDecoder(request.uri(), true);
										Map<String,List<String>> params = decoder.parameters();
										
										System.out.println("//--------------------------------------------------------------------------");
										for(String key : params.keySet()) {
											System.out.println("# GetParam : " + key + " = " + params.get(key));
										}
										
										System.out.println("//--------------------------------------------------------------------------");
										// print body
										ByteBuf bodyBuf = request.content();
										int bodySize = bodyBuf.readableBytes();
										System.out.println("Body Size : " + bodySize);
										
										String responseMessage = ""; 
										if(bodySize > 0) {
											responseMessage = handleMessage(bodyBuf.toString(charset)); 
										}
										

										FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
												HttpResponseStatus.OK, copiedBuffer(responseMessage.getBytes(charset)));

										if (HttpUtil.isKeepAlive(request)) {
											response.headers().set(HttpHeaderNames.CONNECTION,
													HttpHeaderValues.KEEP_ALIVE);
										}
										response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/xml");
										response.headers().set(HttpHeaderNames.CONTENT_LENGTH,
												responseMessage.length());

										ctx.writeAndFlush(response);
									} else {
										super.channelRead(ctx, msg);
									}
								}

								@Override
								public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
									ctx.flush();
								}

								@Override
								public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
										throws Exception {
									ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
											HttpResponseStatus.INTERNAL_SERVER_ERROR,
											copiedBuffer(cause.getMessage().getBytes())));
								}
							});
						}
					}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
			channel = bootstrap.bind(port).sync();
		} catch (final InterruptedException e) {
		}
	}

	public void shutdown() {
		slaveGroup.shutdownGracefully();
		masterGroup.shutdownGracefully();

		try {
			channel.channel().closeFuture().sync();
		} catch (InterruptedException e) {
		}
	}

	public static void main(String[] args) {
		// http://localhost:8080?name=이동훈
		new NettyHttpServer().start(8080);
	}
}
