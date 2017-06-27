/**
 * 
 */
package com.espertech.esper.example.limit.server;

import com.espertech.esper.example.limit.TradeEvent;

/**
 * @author jojo
 * 
 */
public class TradeEventSubscriber {
	public void update(String ticker) {
//		if (EsperCEPProvider.sleepListenerMillis > 0) {
//			try {
//				Thread.sleep(EsperCEPProvider.sleepListenerMillis);
//			} catch (InterruptedException ie) {
//				;
//			}
//		}
	}

	public void update(TradeEvent marketData) {
//		if (EsperCEPProvider.sleepListenerMillis > 0) {
//			try {
//				Thread.sleep(EsperCEPProvider.sleepListenerMillis);
//			} catch (InterruptedException ie) {
//				;
//			}
//		}
	}
	
	/**
	 * 示例中会触发这个update方法
	 * @param count
	 * @param sum
	 */
	public void update(long count, double sum,TradeEvent event) {
		System.out.println("count : [" + count + "]; " + "sum : ["
				+ sum + "]; eventRelNo : [" + event.getEventRelNo() + "]");
	}

	public void update(String ticker, double avg, long count, double sum) {
//		if (EsperCEPProvider.sleepListenerMillis > 0) {
//			try {
//				Thread.sleep(EsperCEPProvider.sleepListenerMillis);
//			} catch (InterruptedException ie) {
//				;
//			}
//		}
	}
}
