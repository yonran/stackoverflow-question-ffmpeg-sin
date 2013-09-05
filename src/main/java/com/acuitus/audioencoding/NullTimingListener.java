package com.acuitus.audioencoding;

public class NullTimingListener implements TimingListener {
	public static final NullTimingListener INSTANCE = new NullTimingListener();
	private NullTimingListener(){}
	@Override public void timeSync(long cumulativeSampleCount, long systemTime) {}
	@Override public void close() throws Exception {}
}