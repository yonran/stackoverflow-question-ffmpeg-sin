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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.type.NullType;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import wraplibvorbis.OggLibrary;
import wraplibvorbis.VorbisLibrary;
import wraplibvorbis.VorbisencLibrary;
import wraplibvorbis.ogg_packet;
import wraplibvorbis.ogg_page;
import wraplibvorbis.ogg_stream_state;
import wraplibvorbis.ogg_sync_state;
import wraplibvorbis.vorbis_block;
import wraplibvorbis.vorbis_comment;
import wraplibvorbis.vorbis_dsp_state;
import wraplibvorbis.vorbis_info;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;


public class SinVorbis {
	public static class VorbisLibs {
		public final OggLibrary libogg;
		public final VorbisLibrary libvorbis;
		public final VorbisencLibrary libvorbisenc;
		public VorbisLibs(OggLibrary libogg, VorbisLibrary libvorbis, VorbisencLibrary libvorbisenc) {
			this.libogg = libogg;
			this.libvorbis = libvorbis;
			this.libvorbisenc = libvorbisenc;
		}
	}
	public static class OggVorbisFactory {
		private final VorbisLibs libs;
		private OggVorbisFactory(VorbisLibs libs) {
			this.libs = libs;
		}
		public static OggVorbisFactory create() throws IOException {
			String osname = System.getProperty("os.name");
			Map<String, String> libFiles = new LinkedHashMap<>();
			String LIBOGG = "libogg", LIBVORBISENC = "libvorbisenc", LIBVORBISFILE = "libvorbisfile", LIBVORBIS = "libvorbis";
			// compiled libraries libogg-1.3.1, libvorbis-1.3.3 
			if (osname.equals("Mac OS X")) {
				libFiles.put(LIBOGG, "resources/osx_x86_64/libogg.0.dylib");
				libFiles.put(LIBVORBIS, "resources/osx_x86_64/libvorbis.0.dylib");
				libFiles.put(LIBVORBISENC, "resources/osx_x86_64/libvorbisenc.2.dylib");
				libFiles.put(LIBVORBISFILE, "resources/osx_x86_64/libvorbisfile.3.dylib");
			} else {
				libFiles.put(LIBOGG, "resources/linux_x86_64/libogg.so.0.8.1");
				libFiles.put(LIBVORBIS, "resources/linux_x86_64/libvorbis.so.0.4.6");
				libFiles.put(LIBVORBISENC, "resources/linux_x86_64/libvorbisenc.so.2.0.9");
				libFiles.put(LIBVORBISFILE, "resources/linux_x86_64/libvorbisfile.so.3.3.5");
			}
			VorbisLibrary libvorbis = null;
			OggLibrary libogg = null;
			VorbisencLibrary libvorbisenc = null;
			Path dir = Files.createTempDirectory("libvorbis");
			dir.toFile().deleteOnExit();
			for (Entry<String, String> entry: libFiles.entrySet()) {
				String libname = entry.getKey();
				String resourcePath = entry.getValue();
				String basename = new File(resourcePath).getName();
				int firstDotPos = basename.indexOf('.');
				String beforeDot = basename.substring(0, firstDotPos);
				String afterDot = basename.substring(firstDotPos);
				File tempFile = Files.createFile(dir.resolve(basename)).toFile();
				tempFile.deleteOnExit();
				try (
					InputStream resourceStream = OggLibrary.class.getResourceAsStream(resourcePath);
					OutputStream tempOutput = new FileOutputStream(tempFile);
					) {
					copy(resourceStream, tempOutput);
				}
				Runtime.getRuntime().load(tempFile.getPath());
//					NativeLibrary.getInstance(tempFile.getPath());
				if (LIBVORBIS.equals(libname)) {
					libvorbis = (VorbisLibrary) Native.loadLibrary(tempFile.getPath(), VorbisLibrary.class);
				} else if (LIBOGG.equals(libname)) {
					libogg = (OggLibrary) Native.loadLibrary(tempFile.getPath(), OggLibrary.class);
				} else if (LIBVORBISENC.equals(libname)) {
					libvorbisenc = (VorbisencLibrary) Native.loadLibrary(tempFile.getPath(), VorbisencLibrary.class);
				}
			}
			return new OggVorbisFactory(new VorbisLibs(libogg, libvorbis, libvorbisenc));
		}
		/**
		 * 
		 * @param out
		 * @param sampleRate samples/s e.g. 44100
		 * @param numChannels e.g. stereo=2
		 * @param baseQuality .4 = 128kbps; .1=80kbps
		 * @return
		 * @throws IOException 
		 */
		public OggVorbisEncoder createEncoder(WritableByteChannel out, int sampleRate, int numChannels, float baseQuality) throws IOException {
			// see http://svn.xiph.org/trunk/vorbis/examples/encoder_example.c
			ogg_stream_state os = new ogg_stream_state(); os.setAutoSynch(false);

			/********* Encode setup *****/
			/** static vorbis bitstream settings */
			vorbis_info vi = new vorbis_info(); vi.setAutoSynch(false);
			libs.libvorbis.vorbis_info_init(vi);
			// Encoder could be initialized in several different ways using vorbis_encode_init, vorbis_encode_ctl. See example.
			int overr;
			if (0 != (overr = libs.libvorbisenc.vorbis_encode_init_vbr(vi, new NativeLong(numChannels), new NativeLong(sampleRate), baseQuality))) {
				throwOvError(overr, "vorbis_encode_init_vbr");
			}

			/** Add a comment */
			/** struct that stores all the user comments */
			vorbis_comment vc = new vorbis_comment(); vc.setAutoSynch(false);
			libs.libvorbis.vorbis_comment_init(vc);
			libs.libvorbis.vorbis_comment_add_tag(vc, "ENCODER", SinVorbis.class.getName());
			libs.libvorbis.vorbis_comment_add_tag(vc, "hello", "world");

			/** Setup the analysis state and auxiliary encoding storage */
			/** central working state for the packet->PCM decoder */
			vorbis_dsp_state vd = new vorbis_dsp_state(); vd.setAutoSynch(false);
			/** local working space for packet->PCM decode */
			vorbis_block vb = new vorbis_block(); vb.setAutoSynch(false);
			libs.libvorbis.vorbis_analysis_init(vd, vi);
			libs.libvorbis.vorbis_block_init(vd, vb);

			/** set up our packet->stream encoder */
			/* pick a random serial number; that way we can more likely build chained streams just by concatenation */
			Random random = new Random(8972341);
			int serialno = random.nextInt();
			if (0 != libs.libogg.ogg_stream_init(os, serialno)) {
				throw new IllegalStateException();
			}

			/*
			 * Vorbis streams begin with three headers; the initial header (with
			 * most of the codec setup parameters) which is mandated by the Ogg
			 * bitstream spec. The second header holds any comment fields. The third
			 * header holds the bitstream codebook. We merely need to make the
			 * headers, then pass them to libvorbis one at a time; libvorbis handles
			 * the additional Ogg bitstream constraints
			 */
			ogg_packet header = new ogg_packet(); header.setAutoSynch(false);
			ogg_packet header_comm = new ogg_packet(); header_comm.setAutoSynch(false);
			ogg_packet header_code = new ogg_packet(); header_code.setAutoSynch(false);
			libs.libvorbis.vorbis_analysis_headerout(vd, vc, header, header_comm, header_code);
			libs.libogg.ogg_stream_packetin(os, header);  // automatically placed in its own page
			libs.libogg.ogg_stream_packetin(os, header_comm);
			libs.libogg.ogg_stream_packetin(os, header_code);
			/** one Ogg bitstream page that will contain Vorbis packets */
			ogg_page og = new ogg_page(); og.setAutoSynch(false);
			/* This ensures the actual audio data will start on a new page, as per spec.*/
			while (0 != libs.libogg.ogg_stream_flush(os, og)) {
				og.read();
				ByteBuffer headerBuffer = og.header.getByteBuffer(0, og.header_len.longValue());
				ByteBuffer bodyBuffer = og.body.getByteBuffer(0, og.body_len.longValue());
				out.write(headerBuffer);
				out.write(bodyBuffer);
			}
			return new OggVorbisEncoder(libs, os, vb, vd, vc, vi, out);
		}
	}
	public static class OggVorbisEncoder implements AutoCloseable {
		private final WritableByteChannel out;
		private final ogg_stream_state os;
		private final vorbis_block vb;
		private final vorbis_dsp_state vd;
		private final vorbis_comment vc;
		private final vorbis_info vi;
		/** Out param of ogg_stream_pageout. Reused for speed. */
		private final ogg_page og;
		/** Out param of vorbis_bitrate_flushpacket. Reused for speed. */
		private final ogg_packet op;
		long audio_time = 0;  // in (1/bitrate) s
		private VorbisLibs libs;
		private OggVorbisEncoder(VorbisLibs libs, ogg_stream_state os, vorbis_block vb, vorbis_dsp_state vd, vorbis_comment vc, vorbis_info vi, WritableByteChannel out) {
			this.libs = Objects.requireNonNull(libs);
			this.os = Objects.requireNonNull(os);
			this.vb = Objects.requireNonNull(vb);
			this.vd = Objects.requireNonNull(vd);
			this.vc = Objects.requireNonNull(vc);
			this.vi = Objects.requireNonNull(vi);
			this.out = Objects.requireNonNull(out);
			/** one Ogg bitstream page that will contain Vorbis packets */
			this.og = new ogg_page(); og.setAutoSynch(false);
			/** One raw packet of data */
			this.op = new ogg_packet(); op.setAutoSynch(false);
		}
		public void write(FloatBuffer... floatBuffers) throws IOException {
			// Allocate (non-interleaved) buffers float[2][1024]
			int numSamples = floatBuffers[0].remaining();
			if (numSamples == 0)
				return;
			Pointer buffers = libs.libvorbis.vorbis_analysis_buffer(vd, numSamples).getPointer();
			for (int channelId = 0; channelId < floatBuffers.length; channelId++) {
				FloatBuffer channel = floatBuffers[channelId];
				Pointer channelPointer = buffers.getPointer(Pointer.SIZE * channelId);
				for (int i = 0; i < numSamples; i++) {
					float sample = channel.get();
					channelPointer.setFloat(i * (Float.SIZE/8), sample);
				}
			}
			audio_time += numSamples;
			int overr;
			/* tell the library how much we actually submitted */
			if ((overr = libs.libvorbis.vorbis_analysis_wrote(vd, numSamples)) < 0) {
				throwOvError(overr, "vorbis_analysis_wrote");
			}
			blockout();
		}
		private void blockout() throws IOException {
			int overr;
			/*
			 * vorbis does some data preanalysis, then divvies up blocks for more
			 * involved (potentially parallel) processing. Get a single block for
			 * encoding now
			 */
			while (1 == (overr = libs.libvorbis.vorbis_analysis_blockout(vd, vb))) {
				/* analysis, assume we want to use bitrate management */
				if ((overr = libs.libvorbis.vorbis_analysis(vb, null)) < 0) {
					throw new IllegalStateException();
				}
				if ((overr = libs.libvorbis.vorbis_bitrate_addblock(vb)) < 0) {
					throw new IllegalStateException();
				}
				while (1 == (overr = libs.libvorbis.vorbis_bitrate_flushpacket(vd, op))) {
					/* weld the packet into the bitstream */
					libs.libogg.ogg_stream_packetin(os, op);
					while (0 != libs.libogg.ogg_stream_pageout(os, og)) {
						/* write out pages (if any) */
						og.read();
						ByteBuffer headerBuffer = og.header.getByteBuffer(0, og.header_len.longValue());
						ByteBuffer bodyBuffer = og.body.getByteBuffer(0, og.body_len.longValue());
						out.write(headerBuffer);
						out.write(bodyBuffer);
					}
				}
				if (overr < 0) {
					throw new IllegalStateException();
				}
			}
			if (overr < 0) {
				throw new IllegalStateException();
			}
		}
		@Override public void close() throws IOException {
			int overr;
			// submit an empty buffer to signal end of input.
			if ((overr = libs.libvorbis.vorbis_analysis_wrote(vd, 0)) < 0) {
				throwOvError(overr, "vorbis_analysis_wrote");
			}
			blockout();
			/* clean up and exit.  vorbis_info_clear() must be called last */
			libs.libogg.ogg_stream_clear(os);
			libs.libvorbis.vorbis_block_clear(vb);
			libs.libvorbis.vorbis_dsp_clear(vd);
			libs.libvorbis.vorbis_comment_clear(vc);
			libs.libvorbis.vorbis_info_clear(vi);
			/* ogg_page and ogg_packet structs always point to storage in libvorbis.
			 * They're never freed or manipulated directly
			 */
			out.close();
		}
	}
	private static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[2048];
		int n;
		while (-1 != (n = is.read(buf))) {
			os.write(buf, 0, n);
		}
	}
	public static void saveSoundWave() throws IOException {
		File f = new File("sinvorbis.ogg");
		OggVorbisFactory factory = OggVorbisFactory.create();
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
		OggVorbisFactory factory = OggVorbisFactory.create();
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
		VorbisLibs libs = OggVorbisFactory.create().libs;

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
	private static void getWavHeader(ByteBuffer buf, int bytesPerMonoSample, int numChannels, int sampleRate, int numSamples) {
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

	public interface AudioEncoder extends AutoCloseable {
		public void gotSample(short... sample) throws IOException;
		public void frameFinished(int totalSampleCount) throws IOException;
	}
	public static class OggVorbisAudioEncoder implements AudioEncoder {
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
		@Override public void frameFinished(int totalSampleCount) throws IOException {
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
	public static class WavAudioEncoder implements AudioEncoder {
		private final RandomAccessFile wavFile;
		private final int numChannels;
		private final int sampleRate;
		private int totalSampleCount;
		private WavAudioEncoder(RandomAccessFile wavFile, int numChannels, int sampleRate) {
			this.wavFile = wavFile;
			this.numChannels = numChannels;
			this.sampleRate = sampleRate;
		}
		public static WavAudioEncoder writeWavHeaderAndCreate(RandomAccessFile wavFile, int numChannels, int sampleRate) throws IOException {
			wavFile.setLength(WAV_HEADER_SIZE);
			wavFile.seek(0);
			ByteBuffer wavHeader = ByteBuffer.allocate(WAV_HEADER_SIZE);
			getWavHeader(wavHeader, Short.SIZE/Byte.SIZE, numChannels, sampleRate, Integer.MAX_VALUE);
			wavHeader.flip();
			wavFile.write(wavHeader.array(), wavHeader.position(), wavHeader.remaining());
			return new WavAudioEncoder(wavFile, numChannels, sampleRate);
		}
		public static void fixWavHeader(RandomAccessFile wavFile, int numChannels, int sampleRate, int numSamples) throws IOException {
			ByteBuffer wavHeader = ByteBuffer.allocate(WAV_HEADER_SIZE);
			getWavHeader(wavHeader, Short.SIZE/Byte.SIZE, numChannels, sampleRate, numSamples);
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
		@Override public void frameFinished(int totalSampleCount) throws IOException {this.totalSampleCount = totalSampleCount;}
		@Override public void close() throws Exception {
			try {
				fixWavHeader(wavFile, numChannels, sampleRate, totalSampleCount);
			} finally {
				wavFile.close();
			}
		}
	}
	public static class AudioRecorder {
		private int numChannels;
		private int sampleRate;
		private final AudioEncoder[] audioEncoders;
		private final AudioRunnable runnable = new AudioRunnable();
		private final AtomicBoolean shouldStop = new AtomicBoolean();
		private final int bufferSampleCount;
		final FutureTask<NullType> finished;
		int numSamplesRecorded;
		private AudioRecorder(int numChannels, int sampleRate, int bufferSampleCount, AudioEncoder... audioEncoders) {
			this.numChannels = numChannels;
			this.sampleRate = sampleRate;
			this.bufferSampleCount = bufferSampleCount;
			this.audioEncoders = audioEncoders;
			finished = new FutureTask<>(this.runnable);
		}
		public static AudioRecorder start(int numChannels, int sampleRate, int bufferSampleCount, AudioEncoder... audioEncoders) {
			AudioRecorder recorder = new AudioRecorder(numChannels, sampleRate, bufferSampleCount, audioEncoders);
			Thread t = new Thread(recorder.finished, "Audio Recorder");
			t.start();
			return recorder;
		}
		public void stop() {
			this.shouldStop.set(true);
		}
		class AudioRunnable implements Callable<NullType> {
			@Override public NullType call() throws Exception {
				ByteOrder byteOrder = ByteOrder.nativeOrder();
				AudioFormat microphoneFormat = new AudioFormat(sampleRate, Short.SIZE, numChannels, true, byteOrder == ByteOrder.BIG_ENDIAN);
				try (TargetDataLine microphoneLine = AudioSystem.getTargetDataLine(microphoneFormat);
					SourceDataLine wavLine = AudioSystem.getSourceDataLine(new AudioFormat(sampleRate, Short.SIZE, numChannels, true, byteOrder == ByteOrder.BIG_ENDIAN));
						) {
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
							int available = microphoneLine.available();
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
								for (AudioEncoder audioEncoder: audioEncoders)
									audioEncoder.frameFinished(numSamplesRecorded);
							}
						}
					} catch (IOException e) {
						throw e;
					}
					System.out.format("Recorded %d samples in %d ms (%d samples/s)", numSamplesRecorded, t1 - t0, t1 == t0 ? -1 : numSamplesRecorded*1000l/(t1 - t0));
				} catch (LineUnavailableException e) {
					throw e;
				} finally {
					for (AudioEncoder encoder: audioEncoders) {
						encoder.close(); // TODO: catch IOException within loop
					}
				}
				return null;
			}
		}
	}
	public static void record(OggVorbisEncoder encoder, RandomAccessFile wavFile) throws LineUnavailableException, IOException, InterruptedException, ExecutionException {
		int numChannels = 2;
		int sampleRate = 44100;
		if (true) {
			AudioRecorder audiorecorder = AudioRecorder.start(numChannels, sampleRate, sampleRate * 1, WavAudioEncoder.writeWavHeaderAndCreate(wavFile, numChannels, sampleRate));
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
