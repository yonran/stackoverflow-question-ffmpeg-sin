package com.acuitus.audioencoding;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.lang.model.type.NullType;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;


public class AudioRecorderLoop {
	private int numChannels;
	private int sampleRate;
	private final AudioEncoder[] audioEncoders;
	private final AudioRunnable runnable = new AudioRunnable();
	private final AtomicBoolean shouldStop = new AtomicBoolean();
	private final int bufferSampleCount;
	public final FutureTask<NullType> finished;
	private final TimingListener timingListener;
	long numSamplesRecorded;
	private AudioRecorderLoop(int numChannels, int sampleRate, int bufferSampleCount, TimingListener timingListener, AudioEncoder... audioEncoders) {
		this.numChannels = numChannels;
		this.sampleRate = sampleRate;
		this.bufferSampleCount = bufferSampleCount;
		this.timingListener = timingListener;
		this.audioEncoders = audioEncoders;
		finished = new FutureTask<>(this.runnable);
	}
	public static AudioRecorderLoop start(int numChannels, int sampleRate, int bufferSampleCount, TimingListener timingListener, AudioEncoder... audioEncoders) {
		AudioRecorderLoop recorder = new AudioRecorderLoop(numChannels, sampleRate, bufferSampleCount, timingListener, audioEncoders);
		Thread t = new Thread(recorder.finished, "Audio Recorder");
		t.start();
		return recorder;
	}
	public void stop() {
		this.shouldStop.set(true);
	}
	private static class FrameAndSystemTime {
		public final long framePosition;
		public final long systemTime;
		public FrameAndSystemTime(long framePosition, long systemTime) {
			this.framePosition = framePosition;
			this.systemTime = systemTime;
		}
	}
	
	/**
	 * A separate thread to synchronize audio sample number with system time.
	 * 
	 * <p>
	 * The read() call in the audio data thread returns every ~500ms. In
	 * contrast, getLongFramePosition() changes only once every ~100ms (on
	 * Linux). So in order to measure audio clock ticks accurately, we have to
	 * poll the audio time in a separate thread at a higher frequency.
	 */
	class DriftSampler implements Callable<NullType> {
		private final DataLine microphoneLine;
		public final AtomicReference<FrameAndSystemTime> frameAndSystemTime;
		public DriftSampler(DataLine dataLine) {
			this.microphoneLine = dataLine;
			this.frameAndSystemTime = new AtomicReference<>(new FrameAndSystemTime(0l,0l));
		}
		@Override public NullType call() {
			while (! shouldStop.get()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					return null;
				}
				// This number jumps by ~100ms at a time.
				long framePos = microphoneLine.getLongFramePosition();
				long systemTime = System.currentTimeMillis();
				FrameAndSystemTime existing = this.frameAndSystemTime.get();
				if (existing.framePosition == framePos)
					continue;
				this.frameAndSystemTime.set(new FrameAndSystemTime(framePos, systemTime));
			}
			return null;
		}
	}
	class AudioRunnable implements Callable<NullType> {
		@Override public NullType call() throws Exception {
			ByteOrder byteOrder = ByteOrder.nativeOrder();
			AudioFormat microphoneFormat = new AudioFormat(sampleRate, Short.SIZE, numChannels, true, byteOrder == ByteOrder.BIG_ENDIAN);
			try (TargetDataLine microphoneLine = AudioSystem.getTargetDataLine(microphoneFormat);
				SourceDataLine wavLine = AudioSystem.getSourceDataLine(new AudioFormat(sampleRate, Short.SIZE, numChannels, true, byteOrder == ByteOrder.BIG_ENDIAN));
					) {
				DriftSampler driftSampler = new DriftSampler(microphoneLine);
				Thread driftThread = new Thread(new FutureTask<>(driftSampler), "Audio Recorder Drift Sampler");
				driftThread.setDaemon(true);
				driftThread.start();
				try {
					int bufferSize = bufferSampleCount * numChannels * Short.SIZE/8;
					ByteBuffer buf = ByteBuffer.allocate(bufferSize);
					buf.order(byteOrder);
					microphoneLine.open();
					long t0 = System.currentTimeMillis();
					long t1 = t0;
					microphoneLine.start();
					short[] sample = new short[numChannels];
					try {
						while (! shouldStop.get()) {
							buf.clear();
							int bytesToRead = buf.remaining(); // Math.min(available, buf.remaining());
							int pos = buf.position();
							int numBytes = microphoneLine.read(buf.array(), pos, bytesToRead);

							t1 = System.currentTimeMillis();
							buf.limit(numBytes);
							int numSamples = numBytes / (Short.SIZE/8 * numChannels);
							if (numSamples > 0) {
								for (int i = 0; i < numSamples; i++) {
									for (int j = 0; j < numChannels; j++) {
										short y = buf.getShort();
										sample[j] = y;
									}
									for (AudioEncoder audioEncoder: audioEncoders) {
										audioEncoder.gotSample(sample);
									}
								}
								numSamplesRecorded += numSamples;
								FrameAndSystemTime framePosAndMicrosecondPos = driftSampler.frameAndSystemTime.get();
								timingListener.timeSync(framePosAndMicrosecondPos.framePosition, framePosAndMicrosecondPos.systemTime);
								for (AudioEncoder audioEncoder: audioEncoders)
									audioEncoder.frameFinished(numSamplesRecorded);
							}
						}
					} catch (IOException e) {
						throw e;
					}
					System.out.format("Recorded %d samples in %d ms (%d samples/s)", numSamplesRecorded, t1 - t0, t1 == t0 ? -1 : numSamplesRecorded*1000l/(t1 - t0));
				} finally {
					driftThread.interrupt();
				}
			} catch (LineUnavailableException e) {
				throw e;
			} finally {
				for (AudioEncoder encoder: audioEncoders) {
					encoder.close(); // TODO: catch IOException within loop
				}
				timingListener.close();
			}
			return null;
		}
	}
}