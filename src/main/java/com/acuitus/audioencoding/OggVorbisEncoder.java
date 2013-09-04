package com.acuitus.audioencoding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;

import wraplibvorbis.OggLibrary;
import wraplibvorbis.VorbisLibrary;
import wraplibvorbis.VorbisencLibrary;
import wraplibvorbis.ogg_packet;
import wraplibvorbis.ogg_page;
import wraplibvorbis.ogg_stream_state;
import wraplibvorbis.vorbis_block;
import wraplibvorbis.vorbis_comment;
import wraplibvorbis.vorbis_dsp_state;
import wraplibvorbis.vorbis_info;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;


public class OggVorbisEncoder implements AutoCloseable {
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
			final VorbisLibs libs;
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
					File tempFile = Files.createFile(dir.resolve(basename)).toFile();
					tempFile.deleteOnExit();
					try (
						InputStream resourceStream = OggLibrary.class.getResourceAsStream(resourcePath);
						OutputStream tempOutput = new FileOutputStream(tempFile);
						) {
						SinVorbis.copy(resourceStream, tempOutput);
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
					SinVorbis.throwOvError(overr, "vorbis_encode_init_vbr");
				}
	
				/** Add a comment */
				/** struct that stores all the user comments */
				vorbis_comment vc = new vorbis_comment(); vc.setAutoSynch(false);
				libs.libvorbis.vorbis_comment_init(vc);
				libs.libvorbis.vorbis_comment_add_tag(vc, "ENCODER", SinVorbis.class.getName());
				libs.libvorbis.vorbis_comment_add_tag(vc, "start_timestamp", String.format("%d", System.currentTimeMillis()));
	
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
	OggVorbisEncoder(VorbisLibs libs, ogg_stream_state os, vorbis_block vb, vorbis_dsp_state vd, vorbis_comment vc, vorbis_info vi, WritableByteChannel out) {
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
			SinVorbis.throwOvError(overr, "vorbis_analysis_wrote");
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
			SinVorbis.throwOvError(overr, "vorbis_analysis_wrote");
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