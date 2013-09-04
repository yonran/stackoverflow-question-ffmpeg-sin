package com.acuitus.audioencoding;

public interface TimingListener extends AutoCloseable {
	public void timeSync(long cumulativeSampleCount, long systemTime);
}