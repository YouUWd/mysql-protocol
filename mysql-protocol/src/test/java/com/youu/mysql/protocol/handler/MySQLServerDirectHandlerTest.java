package com.youu.mysql.protocol.handler;

import java.net.URI;
import java.security.DigestException;

import com.google.common.primitives.Bytes;
import com.mysql.cj.CharsetMapping;
import com.mysql.cj.protocol.Security;
import com.mysql.cj.util.StringUtils;
import com.youu.mysql.protocol.codec.MySQLEncoder;
import com.youu.mysql.protocol.pkg.req.ComQuery;
import com.youu.mysql.protocol.pkg.req.ComQuit;
import com.youu.mysql.protocol.pkg.req.LoginRequest;
import com.youu.mysql.protocol.pkg.res.HandshakePacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class MySQLServerDirectHandlerTest {
    public static final DockerImageName MYSQL_80_IMAGE = DockerImageName.parse("mysql:8.0.25");
    private static MySQLContainer<?> mysql;

    private static final String USER_NAME = "root";
    private static final String PASS_WORD = "pass";
    private static final EventLoopGroup group = new NioEventLoopGroup();

    private final Bootstrap bootstrap = new Bootstrap().group(group)
        .channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<SocketChannel>() {
                     @Override
                     protected void initChannel(SocketChannel ch) {
                         ChannelPipeline pipeline = ch.pipeline();
                         // and then business logic.
                         pipeline.addLast(new MySQLEncoder());
                         pipeline.addLast(new MySQLClientHandler());
                     }
                 }
        );

    @BeforeClass
    public static void init() {
        mysql = new MySQLContainer<>(MYSQL_80_IMAGE)
            .withDatabaseName("test")
            .withUsername(USER_NAME)
            .withPassword(PASS_WORD);
        mysql.start();

    }

    @AfterClass
    public static void destroy() {
        group.shutdownGracefully();
    }

    /**
     * MySQLContainer 默认 caching_sha2_password，目前暂时无法处理
     *
     * @throws InterruptedException
     * @throws DigestException
     */
    @Ignore
    @Test
    public void test() throws InterruptedException, DigestException {
        String jdbcUrl = mysql.getJdbcUrl();
        URI uri = URI.create(jdbcUrl.substring(5));

        // Make a new connection.
        ChannelFuture f = bootstrap.connect(uri.getHost(), uri.getPort()).sync();
        // Get the handler instance to retrieve the answer.
        MySQLClientHandler handler = (MySQLClientHandler)f.channel().pipeline().last();

        ByteBuf handshake = handler.handshake();
        HandshakePacket handshakePacket = new HandshakePacket();
        handshakePacket.read(handshake);

        LoginRequest loginRequest = new LoginRequest();
        String loginHex
            =
            "d900000107a23e01ffffff001c0000000000000000000000000000000000000000000000726f6f74001457a7fdd47410b2a244b2796d529bfa7afdd802946d7973716c5f6e61746976655f70617373776f72640088105f72756e74696d655f76657273696f6e09312e382e305f3233310f5f636c69656e745f76657273696f6e06382e302e32350f5f636c69656e745f6c6963656e73650347504c0f5f72756e74696d655f76656e646f72124f7261636c6520436f72706f726174696f6e0c5f636c69656e745f6e616d65114d7953514c20436f6e6e6563746f722f4a";
        loginRequest.read(Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(loginHex)));

        System.out.println(loginRequest);

        byte[] authPluginDataPart1 = handshakePacket.getAuthPluginDataPart1();
        byte[] authPluginDataPart2 = handshakePacket.getAuthPluginDataPart2();
        byte[] authPluginDataPart2True = new byte[authPluginDataPart2.length - 1];
        System.arraycopy(authPluginDataPart2, 0, authPluginDataPart2True, 0, authPluginDataPart2True.length);

        byte[] seed = Bytes.concat(authPluginDataPart1, authPluginDataPart2True);

        int characterSet = loginRequest.getCharacterSet();
        byte[] passes;
        if ("caching_sha2_password".equals(handshakePacket.getAuthPluginName())) {
            passes = Security
                .scrambleCachingSha2(
                    StringUtils.getBytes(PASS_WORD, CharsetMapping.getJavaEncodingForCollationIndex(characterSet)),
                    seed);
            loginRequest.setAuthPluginName("caching_sha2_password");
        } else {
            passes = Security.scramble411(PASS_WORD, seed,
                CharsetMapping.getJavaEncodingForCollationIndex(characterSet));
        }
        System.out.println(ByteBufUtil.hexDump(passes));
        loginRequest.setAuthResponse(passes);

        ByteBuf buf = handler.execute(loginRequest);
        Assert.assertEquals("0200000201030700000300000002000000", ByteBufUtil.hexDump(buf));

        ComQuery query = new ComQuery();
        query.setQuery("select 1");

        Assert.assertEquals(
            "010000010117000002036465660000000131000c3f000100000008810000000002000003013107000004fe000002000000",
            ByteBufUtil.hexDump(handler.execute(query)));
        handler.execute(new ComQuit());

    }
}