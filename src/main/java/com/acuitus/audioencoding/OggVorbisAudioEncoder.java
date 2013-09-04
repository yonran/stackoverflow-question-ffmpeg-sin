package com.acuitus.audioencoding;
import java.io.IOException;
import java.nio.FloatBuffer;




public class OggVorbisAudioEncoder implements AudioEncoder {
	private final OggVorbisEncoder delegate;
	private final int samplesPerFrame;
	private final FloatBuffer[] floatBuffers;
	public OggVorbisAudioEncoder(OggVorbisEncoder delegate, int numChannels, int samplesPerFrame) {
		this.delegate = delegate;
		this.samplesPerFrame = samplesPerFrame;
		this.floatBuffers = new FloatBuffer[numChannels];
		for (int i = 0; i < numChannels; i++)
			floatBuffers[i] = FloatBuffer.allocate(samplesPerFrame);
	}
	@Override public void gotSample(short... sample) {
		for (int i = 0; i < sample.length; i++) {
			short y = sample[i];
			floatBuffers[i].put(y/32767.f);
		}
	}
	@Override public void frameFinished(long totalSampleCount) throws IOException {
		for (FloatBuffer floatBuffer: floatBuffers)
			floatBuffer.flip();
		delegate.write(floatBuffers);
		for (FloatBuffer floatBuffer: floatBuffers)
			floatBuffer.clear();
	}
	@Override public void close() throws Exception {
		delegate.close();
	}
}