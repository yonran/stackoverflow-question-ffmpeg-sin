package com.acuitus.audioencoding;

public interface TimingListener {
	public void frameReceived(long cumulativeSampleCount, long timestamp);
}