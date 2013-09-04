package com.acuitus.audioencoding;

public class NullTimingListener implements TimingListener {
	public static final NullTimingListener INSTANCE = new NullTimingListener();
	private NullTimingListener(){}
	@Override public void frameReceived(long cumulativeSampleCount, long timestamp) {}
}