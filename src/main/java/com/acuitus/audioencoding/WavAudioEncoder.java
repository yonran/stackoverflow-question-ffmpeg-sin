package com.acuitus.audioencoding;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;


public class WavAudioEncoder implements AudioEncoder {
	private final RandomAccessFile wavFile;
	private final int numChannels;
	private final int sampleRate;
	private long totalSampleCount;
	private WavAudioEncoder(RandomAccessFile wavFile, int numChannels, int sampleRate) {
		this.wavFile = wavFile;
		this.numChannels = numChannels;
		this.sampleRate = sampleRate;
	}
	public static WavAudioEncoder writeWavHeaderAndCreate(RandomAccessFile wavFile, int numChannels, int sampleRate) throws IOException {
		wavFile.setLength(SinVorbis.WAV_HEADER_SIZE);
		wavFile.seek(0);
		ByteBuffer wavHeader = ByteBuffer.allocate(SinVorbis.WAV_HEADER_SIZE);
		SinVorbis.getWavHeader(wavHeader, Short.SIZE/Byte.SIZE, numChannels, sampleRate, Integer.MAX_VALUE);
		wavHeader.flip();
		wavFile.write(wavHeader.array(), wavHeader.position(), wavHeader.remaining());
		return new WavAudioEncoder(wavFile, numChannels, sampleRate);
	}
	public static void fixWavHeader(RandomAccessFile wavFile, int numChannels, int sampleRate, int numSamples) throws IOException {
		ByteBuffer wavHeader = ByteBuffer.allocate(SinVorbis.WAV_HEADER_SIZE);
		SinVorbis.getWavHeader(wavHeader, Short.SIZE/Byte.SIZE, numChannels, sampleRate, numSamples);
		wavHeader.flip();
		wavFile.seek(0);
		wavFile.write(wavHeader.array(), wavHeader.position(), wavHeader.remaining());
	}
	@Override public void gotSample(short... sample) throws IOException {
		for (short y: sample) {
			wavFile.write((byte)y);
			wavFile.write((byte)(y >> 8));
		}
	}
	@Override public void frameFinished(long totalSampleCount) throws IOException {this.totalSampleCount = totalSampleCount;}
	@Override public void close() throws Exception {
		try {
			fixWavHeader(wavFile, numChannels, sampleRate, (int)totalSampleCount);
		} finally {
			wavFile.close();
		}
	}
}