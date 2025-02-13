/**
 * Copyright 2016-2018 Dell Inc. or its subsidiaries. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.emc.ecs.nfsclient.network;

import com.emc.ecs.nfsclient.rpc.Xdr;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.channels.NotYetConnectedException;

/**
 * Each TCP connection has a corresponding ClientIOHandler instance.
 * ClientIOHandler includes the handler to receiving data and connection
 * establishment
 *
 * @author seibed
 */
public class ClientIOHandler extends ChannelInboundHandlerAdapter  {

    /**
     * The usual logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ClientIOHandler.class);

    /**
     * The Netty helper class object.
     */
    private final Bootstrap _clientBootstrap;

    /**
     * The connection instance
     */
    private  Connection _connection;

    /**
     * The only constructor.
     *
     * @param bootstrap
     *            A Netty helper instance.
     */
    public ClientIOHandler(Bootstrap bootstrap,Connection connection) {
        _clientBootstrap = bootstrap;
        _connection =connection;
    }

    /**
     * Convenience getter method.
     *
     * @return The address.
     */
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) _clientBootstrap.config().remoteAddress();
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
       // LOG.info("Connected to: {}", getRemoteAddress());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Connected to: {}", getRemoteAddress());
        }
    }
    private void closeConnection(String messageStart) {
        LOG.warn(messageStart + ": {}", getRemoteAddress());
        _connection.close();
    }

   @Override
   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
       closeConnection("Channel disconnected");
   }
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        closeConnection("Channel closed");
    }
    /**
     * Convenience method to standardize connection closing.We never try to
     * reconnect the tcp connections. the new connection will be launched when
     * new request is received. Reasons:
     * <ol>
     * <li>Portmap service will disconnect a tcp connection once it has been
     * idle for a few seconds.</li>
     * <li>Mounting service is listening to a temporary port, the port will
     * change after nfs server restart.</li>
     * <li>Even Nfs server may be listening to a temporary port.</li>
     * </ol>
     *
     * @param ctx
     *            The channel context.
     * @param msg
     *           The message.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // Extract the byte data from the ByteBuf
        byte[] rpcResponse = (byte[]) msg;

        // Remove marking (if necessary)
        Xdr x = RecordMarkingUtil.removeRecordMarking(rpcResponse);

        // Notify sender of the response
        int xid = x.getXid();
        _connection.notifySender(Integer.valueOf(xid), x);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Do not print exception if it is BindException (as we're trying to find an available port).
        if (cause instanceof java.net.BindException) {
            return;
        }

        LOG.error("Exception on connection to " + getRemoteAddress(), cause);

        // Close the channel unless it's a NotYetConnectedException during connection state.
        if (!(cause instanceof NotYetConnectedException && _connection.getConnectionState().equals(Connection.State.CONNECTING))) {
            ctx.close();
        }
    }
}
