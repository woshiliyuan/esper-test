/**************************************************************************************
 * Copyright (C) 2007 Esper Team. All rights reserved.                                *
 * http://esper.codehaus.org                                                          *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.example.limit;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Random;
import java.util.UUID;

import com.espertech.esper.example.limit.server.Server;

/**
 * The actual event.
 * The time property (ms) is the send time from the client sender, and can be used for end to end latency providing client(s)
 * and server OS clocks are in sync.
 * The inTime property is the unmarshal (local) time (ns).
 *
 * @author Alexandre Vasseur http://avasseur.blogspot.com
 */
public class TradeEvent {

    public final static int SIZE = Symbols.SIZE + Double.SIZE + Integer.SIZE + Long.SIZE;
    private final static Random RANDOM = new Random();
    
    static {
        System.out.println("TradeEvent event = " + SIZE + " bit = " + SIZE/8 + " bytes");
//        System.out.println("  100 Mbit/s <==> " + (int) (100*1024*1024/SIZE/1000) + "k evt/s");
//        System.out.println("    1 Gbit/s <==> " + (int) (1024*1024*1024/SIZE/1000) + "k evt/s");
    }

    private  String ticker;
    private  double price;
    private  int volume;

    private long time;//ms
    private final long inTime;
    private String eventRelNo;
    private long tradeAmount;
    private String custId;
    private String requestDate;
    

    public TradeEvent(String ticker, double price, int volume) {
        this();
        this.ticker = ticker;
        this.price = price;
        this.volume = volume;
    }

    private TradeEvent() {
        this.inTime = System.nanoTime();
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public long getTime() {
        return time;
    }

    private void setTime(long time) {
        this.time = time;
    }

    public long getInTime() {
        return inTime;
    }

    public void toByteBuffer(ByteBuffer b) {
        //symbol
        CharBuffer cb = b.asCharBuffer();
        cb.put(ticker);//we know ticker is a fixed length string
        b.position(b.position() + cb.position() * 2);
        //price, volume
        b.putDouble(price);
        b.putInt(volume);
        // current time ms for end to end latency
        b.putLong(System.currentTimeMillis());
    }

    public static TradeEvent fromByteBuffer(ByteBuffer byteBuffer) {
        TradeEvent md = new TradeEvent();
        //symbol
        char[] ticker = new char[Symbols.LENGTH];
        CharBuffer cb = byteBuffer.asCharBuffer();
        cb.get(ticker);
        md.setTicker(String.valueOf(ticker));
        //price, volume
        byteBuffer.position(byteBuffer.position() + cb.position() * 2);
        md.setPrice(byteBuffer.getDouble());
        md.setVolume(byteBuffer.getInt());
        // time
        md.setTime(byteBuffer.getLong());
        
        md.setCustId("201400_"+RANDOM.nextInt(10));
        md.setTradeAmount(RANDOM.nextInt(50000000));
        md.setRequestDate(Server.CURRENT_DATE);
        
        md.setEventRelNo(UUID.randomUUID().toString());
        
        return md;
    }

    

    public Object clone() throws CloneNotSupportedException {
        return new TradeEvent(ticker, price, volume);
    }

	public long getTradeAmount() {
		return tradeAmount;
	}

	public void setTradeAmount(long tradeAmount) {
		this.tradeAmount = tradeAmount;
	}

	public String getCustId() {
		return custId;
	}

	public void setCustId(String custId) {
		this.custId = custId;
	}

	public String getRequestDate() {
		return requestDate;
	}

	public void setRequestDate(String requestDate) {
		this.requestDate = requestDate;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TradeEvent [");
		if (ticker != null) {
			builder.append("ticker=");
			builder.append(ticker);
			builder.append(", ");
		}
		builder.append("price=");
		builder.append(price);
		builder.append(", volume=");
		builder.append(volume);
		builder.append(", time=");
		builder.append(time);
		builder.append(", inTime=");
		builder.append(inTime);
		builder.append(", tradeAmount=");
		builder.append(tradeAmount);
		builder.append(", ");
		if (custId != null) {
			builder.append("custId=");
			builder.append(custId);
			builder.append(", ");
		}
		if (requestDate != null) {
			builder.append("requestDate=");
			builder.append(requestDate);
		}
		builder.append("]");
		return builder.toString();
	}

	public String getEventRelNo() {
		return eventRelNo;
	}

	public void setEventRelNo(String eventRelNo) {
		this.eventRelNo = eventRelNo;
	}
}


