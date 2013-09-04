package com.acuitus.audioencoding;
import java.io.IOException;


public interface AudioEncoder extends AutoCloseable {
	public void gotSample(short... sample) throws IOException;
	public void frameFinished(long totalSampleCount) throws IOException;
}