/**************************************************************************************
 * Copyright (C) 2007 Esper Team. All rights reserved.                                *
 * http://esper.codehaus.org                                                          *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.example.limit.client;

import com.espertech.esper.example.limit.Symbols;
import com.espertech.esper.example.limit.TradeEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * A thread that sends market data (symbol, volume, price) at the target rate to the remote host
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 */
public class MarketClient extends Thread {
	
	//发送频率的间隔时间
    private static final int INTERVALTIME_1000 = 1000;
	private Client client;
    private TradeEvent market[];

    public MarketClient(Client client) {
        this.client = client;
        market = new TradeEvent[Symbols.SYMBOLS.length];
        for (int i = 0; i < market.length; i++) {
            market[i] = new TradeEvent(Symbols.SYMBOLS[i], Symbols.nextPrice(10), Symbols.nextVolume(10));
        }
        System.out.printf("MarketData with %d symbols\n", market.length);
    }

    public void run() {
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open(new InetSocketAddress(client.host, client.port));
            System.out.printf("Client connected to %s:%d, rate %d msg/s\n", client.host, client.port, client.rate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TradeEvent market[] = this.market;
        int eventPer1s = client.rate ;/// 20;
        int tickerIndex = 0;
        int countLast5s = 0;
        int sleepLast5s = 0;
        long lastThroughputTick = System.currentTimeMillis();
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(TradeEvent.SIZE / 8);
            do {
                long ms = System.currentTimeMillis();
                for (int i = 0; i < eventPer1s; i++) {
                    tickerIndex = tickerIndex % Symbols.SYMBOLS.length;
                    TradeEvent md = market[tickerIndex++];
                    md.setPrice(Symbols.nextPrice(md.getPrice()));
                    md.setVolume(Symbols.nextVolume(10));

                    byteBuffer.clear();
                    md.toByteBuffer(byteBuffer);
                    byteBuffer.flip();
                    socketChannel.write(byteBuffer);

                    countLast5s++;

                    // info 5 * 1E3
                    if (System.currentTimeMillis() - lastThroughputTick > 5 * 1E3) {
                        System.out.printf("Sent %d in %d(ms) avg ns/msg %.0f(ns) avg %d(msg/s) sleep %d(ms)\n",
                                countLast5s,
                                System.currentTimeMillis() - lastThroughputTick,
                                (float) 1E6 * countLast5s / (System.currentTimeMillis() - lastThroughputTick),
                                countLast5s / 5,
                                sleepLast5s
                        );
                        countLast5s = 0;
                        sleepLast5s = 0;
                        lastThroughputTick = System.currentTimeMillis();
                    }
                }

                // rate adjust
                if (System.currentTimeMillis() - ms < INTERVALTIME_1000) {
                    // lets avoid sleeping if == 1ms, lets account 3ms for interrupts
                    long sleep = Math.max(1, (INTERVALTIME_1000 - (System.currentTimeMillis() - ms) - 3));
                    sleepLast5s += sleep;
                    Thread.sleep(sleep);
                }
            } while (true);
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println("Error sending data to server. Did server disconnect?");
        }
    }
}
