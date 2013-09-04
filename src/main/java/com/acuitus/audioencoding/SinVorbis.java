package com.acuitus.audioencoding;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import wraplibvorbis.ogg_packet;
import wraplibvorbis.ogg_page;
import wraplibvorbis.ogg_stream_state;
import wraplibvorbis.ogg_sync_state;
import wraplibvorbis.vorbis_block;
import wraplibvorbis.vorbis_comment;
import wraplibvorbis.vorbis_dsp_state;
import wraplibvorbis.vorbis_info;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;


public class SinVorbis {
	static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[2048];
		int n;
		while (-1 != (n = is.read(buf))) {
			os.write(buf, 0, n);
		}
	}
	public static void saveSoundWave() throws IOException {
		File f = new File("sinvorbis.ogg");
		OggVorbisEncoder.OggVorbisFactory factory = OggVorbisEncoder.OggVorbisFactory.create();
		int sampleRate = 44100;
		try (
				FileOutputStream out = new FileOutputStream(f);
				OggVorbisEncoder encoder = factory.createEncoder(out.getChannel(), sampleRate, 2, .1f)
				) {
			int total_samples = 30 * sampleRate;
			FloatBuffer left = FloatBuffer.allocate(1024), right = FloatBuffer.allocate(1024);
			int audio_time = 0;
			for (int i = 0; i < total_samples; i++) {
				double x = 440*2*Math.PI * audio_time / sampleRate;
				float y = (float) (.3*Math.sin(x));
				left.put(y);
				right.put(y);
				audio_time++;
				if (! left.hasRemaining()) {
					left.flip();
					right.flip();
					encoder.write(left, right);
					left.clear();
					right.clear();
				}
			}
			if (left.position() != 0) {
				encoder.write(left, right);
				left.clear();
				right.clear();
			}
		}
	}
	public static void main(String[] argv) throws IOException, LineUnavailableException, InterruptedException, ExecutionException {
		OggVorbisEncoder.OggVorbisFactory factory = OggVorbisEncoder.OggVorbisFactory.create();
		try (
			FileOutputStream fos = new FileOutputStream("mic.ogg");
			WritableByteChannel out = fos.getChannel();
			OggVorbisEncoder encoder = factory.createEncoder(out, 44100, 2, .1f);
			RandomAccessFile wavFile = new RandomAccessFile(new File("mic.wav"), "rw");
			) {
			record(encoder, wavFile);
		}
	}

	public static void read(OutputStream pcmShortOutput) throws FileNotFoundException, IOException {
		OggVorbisEncoder.VorbisLibs libs = OggVorbisEncoder.OggVorbisFactory.create().libs;

		/** sync and verify incoming physical bitstream */
		ogg_sync_state oy = new ogg_sync_state();
		oy.setAutoSynch(false);
		/** take physical pages, weld into a logical stream of packets */
		ogg_stream_state os = new ogg_stream_state();
		os.setAutoSynch(false);
		/** one Ogg bitstream page. Vorbis packets are inside */
		ogg_page og = new ogg_page();
		og.setAutoSynch(false);
		/** one raw packet of data for decode */
		ogg_packet op = new ogg_packet();
		op.setAutoSynch(false);

		/** struct that stores all the static vorbis bitstream settings */
		vorbis_info vi = new vorbis_info();
		vi.setAutoSynch(false);
		libs.libvorbis.vorbis_info_init(vi);

		/** struct that stores all the bitstream user comments */
		vorbis_comment vc = new vorbis_comment();
		vc.setAutoSynch(false);
		libs.libvorbis.vorbis_comment_init(vc);

		/** central working state for the packet->PCM decoder */
		vorbis_dsp_state vd = new vorbis_dsp_state();
		vd.setAutoSynch(false);
		/** local working space for packet->PCM decode */
		vorbis_block vb = new vorbis_block();
		vb.setAutoSynch(false);

		/**** Decode setup ****/
		libs.libogg.ogg_sync_init(oy); // now we can read pages

		try (FileInputStream is = new FileInputStream("sinvorbis.ogg")) {
			while (true) { // will repeat if bitstream is chained
				/*
				 * grab some data at the head of the stream. We want the first
				 * page (which is guaranteed to be small and only contain the
				 * Vorbis stream initial header) We need the first page to get
				 * the stream serialno.
				 */
				/* submit a 4k block to libvorbis' Ogg layer */
				Pointer buffer = libs.libogg.ogg_sync_buffer(oy,
						new NativeLong(4096));
				ByteBuffer bufferBuffer = buffer.getByteBuffer(0, 4096);
				is.getChannel().read(bufferBuffer);
				int bytes = bufferBuffer.position();
				libs.libogg.ogg_sync_wrote(oy, new NativeLong(bytes));

				// get the first page
				if (1 != libs.libogg.ogg_sync_pageout(oy, og)) {
					/* have we simply run out of data? If so, we're done. */
					if (bytes < 4096)
						break;
					/* error case. Must not be Vorbis data */
					throw new IllegalStateException(
							"Input does not appear to be an Ogg bitstream.");
				}
				/* Get the serial number and set up the rest of decode. */
				/* serialno first; use it to set up a logical stream */
				libs.libogg.ogg_stream_init(os,
						libs.libogg.ogg_page_serialno(og));
				/*
				 * extract the initial header from the first page and verify
				 * that the Ogg bitstream is in fact Vorbis data
				 */
				/*
				 * I handle the initial header first instead of just having the
				 * code read all three Vorbis headers at once because reading
				 * the initial header is an easy way to identify a Vorbis
				 * bitstream and it's useful to see that functionality seperated
				 * out.
				 */

				libs.libvorbis.vorbis_info_init(vi);
				libs.libvorbis.vorbis_comment_init(vc);
				if (libs.libogg.ogg_stream_pagein(os, og) < 0) {
					/* error; stream version mismatch perhaps */
					throw new IllegalStateException(
							"Error reading first page of Ogg bitstream data.");
				}

				if (libs.libogg.ogg_stream_packetout(os, op) != 1) {
					/* no page? must not be vorbis */
					throw new IllegalStateException(
							"Error reading initial header packet.");
				}

				if (libs.libvorbis.vorbis_synthesis_headerin(vi, vc, op) < 0) {
					/* error case; not a vorbis header */
					throw new IllegalStateException(
							"This Ogg bitstream does not contain Vorbis audio data.");
				}

				/*
				 * At this point, we're sure we're Vorbis. We've set up the
				 * logical (Ogg) bitstream decoder. Get the comment and codebook
				 * headers and set up the Vorbis decoder
				 */

				/*
				 * The next two packets in order are the comment and codebook
				 * headers. They're likely large and may span multiple pages.
				 * Thus we read and submit data until we get our two packets,
				 * watching that no pages are missing. If a page is missing,
				 * error out; losing a header page is the only place where
				 * missing data is fatal.
				 */

				int i = 0;
				while (i < 2) {
					while (i < 2) {
						int result = libs.libogg.ogg_sync_pageout(oy, og);
						if (result == 0)
							break; /* Need more data */
						/*
						 * Don't complain about missing or corrupt data yet.
						 * We'll catch it at the packet output phase
						 */
						if (result == 1) {
							/*
							 * we can ignore any errors here as they'll also
							 * become apparent at packetout
							 */
							libs.libogg.ogg_stream_pagein(os, og);
							while (i < 2) {
								result = libs.libogg.ogg_stream_packetout(os,
										op);
								if (result == 0)
									break;
								if (result < 0) {
									/*
									 * Uh oh; data at some point was corrupted
									 * or missing! We can't tolerate that in a
									 * header. Die.
									 */
									throw new IllegalStateException("Corrupt secondary header.  Exiting.");
								}
								result = libs.libvorbis
										.vorbis_synthesis_headerin(vi, vc, op);
								if (result < 0) {
									throw new IllegalStateException("Corrupt secondary header.  Exiting.");
								}
								i++;
							}
						}
					}
					/* no harm in not checking before adding more */
					buffer = libs.libogg.ogg_sync_buffer(oy, new NativeLong(
							4096));
					bufferBuffer = buffer.getByteBuffer(0, 4096);
					is.getChannel().read(bufferBuffer);
					bytes = bufferBuffer.position();
					if (bytes == 0 && i < 2) {
						throw new IllegalStateException(
								"End of file before finding all Vorbis headers!");
					}
					libs.libogg.ogg_sync_wrote(oy, new NativeLong(bytes));
				}

				/*
				 * Throw the comments plus a few lines about the bitstream we're
				 * decoding
				 */
				{
					vc.readField("user_comments");
					Pointer[] commentsArray = vc.user_comments
							.getPointerArray(0);
					for (Pointer commentPtr : commentsArray) {
						String comment = commentPtr.getString(0);
						System.out.println(comment);
					}
					vi.read();
					System.out.format("\nBitstream is %d channel, %dHz\n",
							vi.channels, vi.rate.longValue());
					System.out.format("Encoded by: %s\n\n", vc.vendor);
				}

				int convsize = 4096 / vi.channels;

				/*
				 * OK, got and parsed all three headers. Initialize the Vorbis
				 * packet->PCM decoder.
				 */
				if (libs.libvorbis.vorbis_synthesis_init(vd, vi) == 0) { /* central decode state */
					/*
					 * local state for most of the decode so multiple block
					 * decodes can proceed in parallel. We could init multiple
					 * vorbis_block structures for vd here
					 */
					libs.libvorbis.vorbis_block_init(vd, vb);

					/*
					 * The rest is just a straight decode loop until end of
					 * stream
					 */
					boolean eos = false;
					while (!eos) {
						while (!eos) {
							int result = libs.libogg.ogg_sync_pageout(oy, og);
							if (result == 0)
								break; /* need more data */
							if (result < 0) { /*
											 * missing or corrupt data at this
											 * page position
											 */
								System.out.format("Corrupt or missing data in bitstream; continuing...\n");
							} else {
								/*
								 * can safely ignore errors at this point
								 */
								libs.libogg.ogg_stream_pagein(os, og);
								while (true) {
									result = libs.libogg.ogg_stream_packetout(os, op);

									if (result == 0)
										break; /* need more data */
									if (result < 0) { /*
													 * missing or corrupt data
													 * at this page position
													 */
										/*
										 * no reason to complain; already
										 * complained above
										 */
									} else {
										/* we have a packet. Decode it */
										int samples;

										if (libs.libvorbis.vorbis_synthesis(vb, op) == 0) /* test for success! */
											libs.libvorbis.vorbis_synthesis_blockin(vd, vb);
										/*
										 * 
										 * *pcm is a multichannel float vector.
										 * In stereo, for example, pcm[0] is
										 * left, and pcm[1] is right. samples is
										 * the size of each channel. Convert the
										 * float values (-1.<=range<=1.) to
										 * whatever PCM format and write it out
										 */

										PointerByReference pcm = new PointerByReference(); // float***
										while ((samples = libs.libvorbis.vorbis_synthesis_pcmout(vd, pcm)) > 0) {
											int j;
											boolean clipflag = false;
											int bout = Math.min(samples, convsize);

											/*
											 * convert floats to 16 bit signed
											 * ints (little-endian) and
											 * interleave
											 */
											ByteBuffer outBuffer = ByteBuffer.allocate(4096 * Short.SIZE / 8);
											outBuffer.order(ByteOrder.LITTLE_ENDIAN);
											Pointer pcmPointer = pcm.getValue(); // float**
											Pointer[] monoPointers = pcmPointer.getPointerArray(0, vi.channels);
											for (j = 0; j < bout; j++) {
												for (i = 0; i < vi.channels; i++) {
													Pointer monoPointer = monoPointers[i]; // float[]
													float monoSample = monoPointer.getFloat((Float.SIZE / 8) * j);
													int val = (int) (monoSample * 32767.f * .5f);
													/*
													 * might as well guard against clipping
													 */
													if (val > 32767) {
														val = 32767;
														clipflag = true;
													}
													if (val < -32768) {
														val = -32768;
														clipflag = true;
													}
													// interleave PCM data
													// index j * vi.channels + i
													outBuffer.putShort((short) val);
												}
											}

											if (clipflag)
												System.err.format("Clipping in frame %d\n", vd.sequence);
											outBuffer.flip();
											pcmShortOutput.write(outBuffer.array(), outBuffer.position(), outBuffer.remaining());

											libs.libvorbis.vorbis_synthesis_read(vd, bout); /*
																	 * tell
																	 * libvorbis
																	 * how many
																	 * samples
																	 * we
																	 * actually
																	 * consumed
																	 */
										}
									}
								}
								if (0 != libs.libogg.ogg_page_eos(og))
									eos = true;
							}
						}
						if (!eos) {

							buffer = libs.libogg.ogg_sync_buffer(oy, new NativeLong(4096));
							bufferBuffer = buffer.getByteBuffer(0, 4096);
							is.getChannel().read(bufferBuffer);
							bytes = bufferBuffer.position();
							libs.libogg.ogg_sync_wrote(oy, new NativeLong(bytes));
							if (bytes == 0)
								eos = true;
						}
					}

					/*
					 * ogg_page and ogg_packet structs always point to storage
					 * in libvorbis. They're never freed or manipulated directly
					 */

					libs.libvorbis.vorbis_block_clear(vb);
					libs.libvorbis.vorbis_dsp_clear(vd);
				} else {
					System.err.format("Error: Corrupt header during playback initialization.\n");
				}

				/*
				 * clean up this logical bitstream; before exit we see if we're
				 * followed by another [chained]
				 */

				libs.libogg.ogg_stream_clear(os);
				libs.libvorbis.vorbis_comment_clear(vc);
				libs.libvorbis.vorbis_info_clear(vi); /* must be called last */
			}
		} finally {
			/* OK, clean up the framer */
			libs.libogg.ogg_sync_clear(oy);
		}
		libs.libvorbis.vorbis_synthesis_headerin(vi, vc, op);
	}

	public static final int WAV_HEADER_SIZE = 44;
	/**
	 * Fill buf with the wav header. buf must have at least 44 bytes remaining.
	 * buf's position will be advanced by 44.
	 * 
	 * @see https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
	 * @param bytesPerMonoSample
	 *            Size for each sample of each channel (bytes) e.g. 2 = short
	 * @param numChannels
	 *            number of channels; e.g., 2 = stereo
	 * @param sampleRate
	 *            sample rate (samples/s) e.g. 44100
	 * @param numSamples
	 *            number of samples in the file. Most players will be able to
	 *            play regardless of this value.
	 */
	static void getWavHeader(ByteBuffer buf, int bytesPerMonoSample, int numChannels, int sampleRate, int numSamples) {
		Charset ascii = Charset.forName("ASCII");
		short audioFormat = 1;  // constant for PCM
		short blockAlign = (short) (numChannels * bytesPerMonoSample);  // bytes/sample
		int byteRate = sampleRate * blockAlign;  // bytes/s
		int fmtSubchunkBodySize = 16;
		int dataSubchunkBodySize = numSamples * blockAlign;
		int chunkSize = 4 + 8 + fmtSubchunkBodySize + 8 + dataSubchunkBodySize;
		assert 8 + 4 + 8 + fmtSubchunkBodySize + 8 == WAV_HEADER_SIZE;
		int initialPos = buf.position();
		buf.order(ByteOrder.LITTLE_ENDIAN);

		buf.put("RIFF".getBytes(ascii));
		buf.putInt(chunkSize);
		buf.put("WAVE".getBytes(ascii));

		// First subchunk "fmt "
		buf.put("fmt ".getBytes(ascii));
		buf.putInt(fmtSubchunkBodySize);
		int subchunkBodyStart = buf.position();
		buf.putShort(audioFormat);
		buf.putShort((short) numChannels);
		buf.putInt(sampleRate);
		buf.putInt(byteRate);
		buf.putShort(blockAlign);
		buf.putShort((short) (bytesPerMonoSample * 8));
		int subchunkBodySize = buf.position() - subchunkBodyStart;
		assert fmtSubchunkBodySize == subchunkBodySize;

		// Second subchunk "data"
		buf.put("data".getBytes(ascii));
		buf.putInt(dataSubchunkBodySize);

		assert buf.position() == initialPos + WAV_HEADER_SIZE;
	}

	public static void record(OggVorbisEncoder encoder, RandomAccessFile wavFile) throws LineUnavailableException, IOException, InterruptedException, ExecutionException {
		int numChannels = 2;
		int sampleRate = 44100;
		if (true) {
			AudioRecorderLoop audiorecorder = AudioRecorderLoop.start(numChannels, sampleRate, sampleRate * 1, NullTimingListener.INSTANCE, WavAudioEncoder.writeWavHeaderAndCreate(wavFile, numChannels, sampleRate));
			Thread.sleep(10000);
			audiorecorder.stop();
			audiorecorder.finished.get();
//			WavAudioEncoder.fixWavHeader(wavFile, numChannels, sampleRate, (int) audiorecorder.numSamplesRecorded);
		} else {
		ByteOrder byteOrder = ByteOrder.nativeOrder();
		AudioFormat microphoneFormat = new AudioFormat(sampleRate, Short.SIZE, numChannels, true, byteOrder == ByteOrder.BIG_ENDIAN);
//		/janew Dataline.Info(TargetDataLine.class, microphoneFormat);
		try (TargetDataLine microphoneLine = AudioSystem.getTargetDataLine(microphoneFormat);
			SourceDataLine wavLine = AudioSystem.getSourceDataLine(new AudioFormat(sampleRate, Short.SIZE, numChannels, true, byteOrder == ByteOrder.BIG_ENDIAN));
				) {
			int bufferSize = Math.min(sampleRate * numChannels * Short.SIZE/8, microphoneLine.getBufferSize());
			int bufferSampleCount = bufferSize / (Short.SIZE/8 * numChannels);
			ByteBuffer buf = ByteBuffer.allocate(bufferSize);
			buf.order(byteOrder);
			FloatBuffer[] floatBuffers = new FloatBuffer[numChannels];
			for (int i = 0; i < numChannels; i++)
				floatBuffers[i] = FloatBuffer.allocate(bufferSampleCount);
			long numSamplesRecorded = 0;
			microphoneLine.open();
			long t0 = System.currentTimeMillis();
			long t1 = t0;
			ByteBuffer wavHeader = ByteBuffer.allocate(WAV_HEADER_SIZE);
			getWavHeader(wavHeader, Short.SIZE/Byte.SIZE, numChannels, sampleRate, Integer.MAX_VALUE);
			wavHeader.flip();
			if (wavFile != null)
				wavFile.write(wavHeader.array(), wavHeader.position(), wavHeader.remaining());
			microphoneLine.start();
			while (numSamplesRecorded < 1 * sampleRate) {
				buf.clear();
				for (FloatBuffer floatBuf: floatBuffers)
					floatBuf.clear();
				int available = microphoneLine.available();
				int bytesToRead = Math.min(available, buf.remaining());
				byte[] array = buf.array();
				int pos = buf.position();
				int numBytes = microphoneLine.read(array, pos, bytesToRead);
				t1 = System.currentTimeMillis();
				buf.limit(numBytes);
				int numSamples = numBytes / (Short.SIZE/8 * numChannels);
				if (numSamples > 0) {
					for (int i = 0; i < numSamples; i++) {
						for (int j = 0; j < numChannels; j++) {
							short y = buf.getShort();
							floatBuffers[j].put(y/32767.f);
							if (wavFile != null) {
								wavFile.write((byte)y);
								wavFile.write((byte)(y >> 8));
							}
						}
					}
					for (FloatBuffer floatBuffer: floatBuffers)
						floatBuffer.flip();
					if (encoder != null)
						encoder.write(floatBuffers);
				}
				numSamplesRecorded += numSamples;
			}
			wavHeader.clear();
			getWavHeader(wavHeader, Short.SIZE/8, numChannels, sampleRate, (int)numSamplesRecorded);
			wavHeader.flip();
			if (wavFile != null) {
				wavFile.seek(0);
				wavFile.write(wavHeader.array(), wavHeader.position(), wavHeader.remaining());
			}
			System.out.format("Recorded %d samples in %d ms (%d samples/s)", numSamplesRecorded, t1 - t0, numSamplesRecorded*1000/(t1 - t0));
		}
		}
	}
	public static void play() throws FileNotFoundException, IOException, InterruptedException, LineUnavailableException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		read(outputStream);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		
		System.out.println(new AudioFormat(44100, Short.SIZE, 2, true, false));
		System.out.println(new AudioFormat(Encoding.PCM_SIGNED, 44100, Short.SIZE, 2, Short.SIZE/8 * 2, 44100, false));
		AudioInputStream ais = new AudioInputStream(inputStream, new AudioFormat(Encoding.PCM_SIGNED, 44100, Short.SIZE, 2, Short.SIZE/8 * 2, 44100, false), outputStream.toByteArray().length);
		if (true) {
			Clip line = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, ais.getFormat()));
			// dump bytes into line
			line.open(ais);
			line.start();
			Thread.sleep(100000);
		} else {
			SourceDataLine sdl = AudioSystem.getSourceDataLine(ais.getFormat());
			sdl.write(outputStream.toByteArray(), 0, outputStream.toByteArray().length);
			sdl.start();
		}
	}
	
	static void throwOvError(int overr, String task) {
		throw new IllegalStateException("" + overr + " while " + task);
	}
}
