package com.acuitus.audioencoding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import wrapffmpeg.AVCodec;
import wrapffmpeg.AVCodecContext;
import wrapffmpeg.AVFormatContext;
import wrapffmpeg.AVFrame;
import wrapffmpeg.AVIOContext;
import wrapffmpeg.AVOutputFormat;
import wrapffmpeg.AVPacket;
import wrapffmpeg.AVStream;
import wrapffmpeg.AvcodecLibrary;
import wrapffmpeg.AvformatLibrary;
import wrapffmpeg.AvutilLibrary;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
	
public class Sin {
	/**
	 * <p>
	 * I started with this JNAerator command, and then I did the following modifications<br>
	 * java -jar ~/Downloads/jnaerator-0.12-SNAPSHOT-20130727.jar  -o wrap-ffmpeg -v -noJar -noComp -package wrapffmpeg -runtime JNA  -library avutil /usr/include/libavutil/avutil.h /usr/include/libavutil/mathematics.h -library avcodec /usr/include/libavcodec/avcodec.h -library avformat /usr/include/libavformat/avformat.h
	 * 
	 * java -jar ~/Downloads/jnaerator-0.12-SNAPSHOT-20130727.jar  -mode Directory -noPrimitiveArrays -nocpp -parseChunks -o wrap-ffmpeg -v -package 'wrapffmpeg' -runtime JNA -I/home/yonran/Downloads/ffmpeg   -library avutil ~/Downloads/ffmpeg/libavutil/avutil.h ~/Downloads/ffmpeg/libavutil/mathematics.h -library avcodec ~/Downloads/ffmpeg/libavcodec/avcodec.h  -library avformat ~/Downloads/ffmpeg/libavformat/avformat.h
	 * ./configure --disable-everything --disable-filters --disable-protocols --disable-demuxers --enable-encoder=vorbis --enable-parser=vorbis --enable-decoder=vorbis  --enable-muxer=ogg --enable-demuxer=ogg --enable-shared --enable-pic --disable-mmx
	 * make
	 * 
	 * <ul>
	 * <li>Make {@link AVFormatContext#chapters} Pointer instead of some struct[]. When we create the struct, we can create these arrays. But when the library creates the struct, we have to read them as pointers. 
	 * <li>Make {@link AVCodec#name} and {@link AVCodec#long_name} String for ease of debugging.
	 * <li>Make {@link AVOutputFormat#name}, {@link AVOutputFormat#long_name} String
	 * <li>Change {@link AvformatLibrary#av_oformat_next(AVOutputFormat)} to return a ByReference.
	 * <li>Change {@link AvformatLibrary#avio_alloc_context(ByteBuffer, int, int, Pointer, avio_alloc_context_read_packet_callback, avio_alloc_context_write_packet_callback, avio_alloc_context_seek_callback)} to return a ByReference.
	 * <li>Make {@link AVCodecContext#time_base} a ByReference.
	 * <li>Add {@link AvcodecLibrary#avcodec_fill_audio_frame(AVFrame, int, int, ByteBuffer, int, int)}
	 * <li>Add {@link AvutilLibrary#av_free(AVFrame)}
	 * </ul>
	 */
	public static List<Object> stuff = new ArrayList<>();
	/**
	 * Abstract class that allows you to put the initialization and cleanup
	 * code at the same place instead of separated by the big try block.
	 */
	public static abstract class SharedPtr<T> implements AutoCloseable {
		public T ptr;
		public SharedPtr(T ptr) {
			this.ptr = ptr;
		}
		/**
		 * Abstract override forces method to throw no checked exceptions.
		 * Subclasses will call a C function that throws no exceptions.
		 */
		@Override public abstract void close();
	}

	private static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[2048];
		int n;
		while (-1 != (n = is.read(buf))) {
			os.write(buf, 0, n);
		}
	}
	/**
	 * @param args
	 * @throws IOException 
	 * @throws LineUnavailableException 
	 */
	public static void main(String[] args) throws IOException, LineUnavailableException {
		File avcodecFile = File.createTempFile("avcodec", ".so");
		File avformatFile = File.createTempFile("avformat", ".so");
		File avutilFile = File.createTempFile("avutil", ".so");
		avcodecFile.deleteOnExit();
		avformatFile.deleteOnExit();
		avutilFile.deleteOnExit();
		try (
			InputStream avcodecStream = Sin.class.getResourceAsStream("wrapffmpeg/resources/linux_x86_64/libavcodec.so");
			InputStream avformatStream = Sin.class.getResourceAsStream("wrapffmpeg/resources/linux_x86_64/libavformat.so");
			InputStream avutilStream = Sin.class.getResourceAsStream("wrapffmpeg/resources/linux_x86_64/libavutil.so");
			OutputStream avcodecOutput = new FileOutputStream(avcodecFile);
			OutputStream avformatOutput = new FileOutputStream(avformatFile);
			OutputStream avutilOutput = new FileOutputStream(avutilFile);
			) {
			copy(avcodecStream, avcodecOutput);
			copy(avformatStream, avformatOutput);
			copy(avutilStream, avutilOutput);
		}
		final AvutilLibrary avutil = (AvutilLibrary) Native.loadLibrary(avutilFile.getPath(), AvutilLibrary.class);;
		final AvcodecLibrary avcodec = (AvcodecLibrary) Native.loadLibrary(avcodecFile.getPath(), AvcodecLibrary.class);
		final AvformatLibrary avformat = (AvformatLibrary) Native.loadLibrary(avformatFile.getPath(), AvformatLibrary.class);
		avcodec.avcodec_register_all();
		avformat.av_register_all();
		AVOutputFormat.ByReference format = null;
		String format_name = "ogg", file_url = "file:sinjava.ogg";
		for (AVOutputFormat.ByReference formatIter = avformat.av_oformat_next(null); formatIter != null; formatIter = avformat.av_oformat_next(formatIter)) {
			formatIter.setAutoWrite(false);
			String iterName = formatIter.name;
			if (format_name.equals(iterName)) {
				format = formatIter;
//				break;
			}
		}
		Objects.requireNonNull(format);
		System.out.format("Found format %s with audio codec %d%n", format_name, format.audio_codec);
		//		AVCodec codec = avcodec.avcodec_find_encoder(format.audio_codec);  // one of AvcodecLibrary.CodecID
		AVCodec codec = null;
		for (AVCodec codecIter = avcodec.av_codec_next(null); codecIter != null; codecIter = avcodec.av_codec_next(codecIter)) {
			codecIter.setAutoWrite(false);
			String name = codecIter.name, long_name = codecIter.long_name;
			System.out.format("Codec: %d: %s %s%n", codecIter.id, name, codecIter.long_name);
			if ("vorbis".equals(name) && codecIter.encode2 != null) {
				codec = codecIter;
			}
		}
		Objects.requireNonNull(codec);
		codec.setAutoWrite(false);
		try (
			SharedPtr<AVFormatContext> fmtCtxPtr = new SharedPtr<AVFormatContext>(avformat.avformat_alloc_context()) {@Override public void close(){if (null!=ptr) avformat.avformat_free_context(ptr);}};
			) {
			AVFormatContext fmtCtx = Objects.requireNonNull(fmtCtxPtr.ptr, "Could not allocate format context");
			fmtCtx.setAutoWrite(false);
			fmtCtx.setAutoRead(false);
			fmtCtx.oformat = format; fmtCtx.writeField("oformat");
			fmtCtx.audio_codec_id = codec.id; fmtCtx.writeField("audio_codec_id");

			AVStream st = avformat.avformat_new_stream(fmtCtx, codec);
			if (null == st)
				throw new IllegalStateException();
			AVCodecContext c = Objects.requireNonNull(st.codec, "Format context should have contained a codec context.");
			st.setAutoWrite(false);
			fmtCtx.readField("nb_streams");
			st.id = fmtCtx.nb_streams - 1; st.writeField("id");
			assert st.id >= 0;
			System.out.format("New stream: id=%d%n", st.id);

			if (0 != (format.flags & AvformatLibrary.AVFMT_GLOBALHEADER)) {
				c.flags |= AvcodecLibrary.CODEC_FLAG_GLOBAL_HEADER;
			}
			c.writeField("flags");

			c.bit_rate = 64000; c.writeField("bit_rate");
			int bestSampleRate;
			if (null == codec.supported_samplerates) {
				bestSampleRate = 44100;
			} else {
				bestSampleRate = 0;
				for (int offset = 0, sample_rate = codec.supported_samplerates.getInt(offset); sample_rate != 0; codec.supported_samplerates.getInt(++offset)) {
					bestSampleRate = Math.max(bestSampleRate, sample_rate);
				}
				assert bestSampleRate > 0;
			}
			c.sample_rate = bestSampleRate; c.writeField("sample_rate");
			c.channel_layout = AvutilLibrary.AV_CH_LAYOUT_STEREO; c.writeField("channel_layout");
			c.channels = avutil.av_get_channel_layout_nb_channels(c.channel_layout); c.writeField("channels");
			assert 2 == c.channels;
			c.sample_fmt = AvutilLibrary.AVSampleFormat.AV_SAMPLE_FMT_S16; c.writeField("sample_fmt");
			c.time_base.num = 1;
			c.time_base.den = bestSampleRate;
			c.writeField("time_base");
			c.setAutoWrite(false);

			AudioFormat javaSoundFormat = new AudioFormat(bestSampleRate, Short.SIZE, c.channels, true, ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
			DataLine.Info javaDataLineInfo = new DataLine.Info(TargetDataLine.class, javaSoundFormat);
			if (! AudioSystem.isLineSupported(javaDataLineInfo))
				throw new IllegalStateException();
			int err;
			if ((err = avcodec.avcodec_open2(c, codec, null)) < 0) {
				throw new IllegalStateException();
			}
			assert c.channels != 0;

			AVIOContext.ByReference[] ioCtxReference = new AVIOContext.ByReference[1];
			if (0 != (err = avformat.avio_open(ioCtxReference, file_url, AvformatLibrary.AVIO_FLAG_WRITE))) {
				throw new IllegalStateException("averror " + err);
			}
			try (
				SharedPtr<AVIOContext.ByReference> ioCtxPtr = new SharedPtr<AVIOContext.ByReference>(ioCtxReference[0]) {@Override public void close(){if (null!=ptr) avutil.av_free(ptr.getPointer());}}
				) {
				AVIOContext.ByReference ioCtx = Objects.requireNonNull(ioCtxPtr.ptr);
				fmtCtx.pb = ioCtx; fmtCtx.writeField("pb");
				int averr = avformat.avformat_write_header(fmtCtx, null);
				if (averr < 0) {
					throw new IllegalStateException("" + averr);
				}
				st.read();  // it is modified by avformat_write_header
				System.out.format("Wrote header. fmtCtx->nb_streams=%d, st->time_base=%d/%d; st->avg_frame_rate=%d/%d%n", fmtCtx.nb_streams, st.time_base.num, st.time_base.den, st.avg_frame_rate.num, st.avg_frame_rate.den); 
				avformat.avio_flush(ioCtx);
				int frame_size = c.frame_size != 0 ? c.frame_size : 384;
				int expectedBufferSize = frame_size * c.channels * (Short.SIZE/8);
				boolean supports_small_last_frame = c.frame_size == 0 ? true : 0 != (codec.capabilities & AvcodecLibrary.CODEC_CAP_SMALL_LAST_FRAME);
				int bufferSize = avutil.av_samples_get_buffer_size((IntBuffer)null, c.channels, frame_size, c.sample_fmt, 1);
				assert bufferSize == expectedBufferSize: String.format("expected %d; got %d", expectedBufferSize, bufferSize);
				ByteBuffer samples = ByteBuffer.allocateDirect(expectedBufferSize);
				samples.order(ByteOrder.nativeOrder());
				int audio_time = 0;  // unit: (c.time_base) s = (1/c.sample_rate) s
				int audio_sample_count = supports_small_last_frame ?
					30 * c.sample_rate :
					30 * c.sample_rate / frame_size * frame_size;
//					audio_sample_count = 4*frame_size;
				while (audio_time < audio_sample_count) {
					int frame_audio_time = audio_time;
					samples.clear();
					int nb_samples_in_frame = 0;
					// encode a single tone sound
					boolean isIncreasing = true;
					short z = 0;
					for (; samples.hasRemaining() && audio_time < audio_sample_count; nb_samples_in_frame++, audio_time++) {
						if (true) {
							double freq = 440 * (1 + 2.*audio_time/audio_sample_count);
							double x = 2*Math.PI*freq/c.sample_rate * audio_time;
							double y = 10000 * Math.sin(x);
							z = (short) y;
							double freq2 = 442 * (1 + 2.*audio_time/audio_sample_count);
							double y2 = 10000 * Math.sin(2*Math.PI*freq2/c.sample_rate * audio_time);
							samples.putShort(z);
							samples.putShort((short) y2);
						} else {
							if (isIncreasing) {
								z++;
								if (z == Short.MAX_VALUE)
									isIncreasing = false;
							} else {
								z--;
								if (z == Short.MIN_VALUE)
									isIncreasing = true;
							}
							samples.putShort(z);
							samples.putShort(z);
						}
					}
					samples.flip();
					try (
							SharedPtr<AVFrame> framePtr = new SharedPtr<AVFrame>(avcodec.avcodec_alloc_frame()) {@Override public void close() {if (null!=ptr) avutil.av_free(ptr.getPointer());}};
							) {
						AVFrame frame = Objects.requireNonNull(framePtr.ptr);
						frame.setAutoRead(false);  // will be an in param
						frame.setAutoWrite(false);
						frame.nb_samples = nb_samples_in_frame; frame.writeField("nb_samples"); // doc says it is unused during encoding, but it actually IS used during avcodec_fill_audio_frame!
						// Presentation time, in AVStream.time_base units.
						frame.pts = avutil.av_rescale_q(frame_audio_time, c.time_base, st.time_base);  // i * codec_time_base / st_time_base
						frame.writeField("pts");
						byte[] tmp = new byte[64];

						assert c.channels > 0;
						int bytesPerSample = avutil.av_get_bytes_per_sample(c.sample_fmt);
						assert bytesPerSample > 0;
						if (0 > (averr = avcodec.avcodec_fill_audio_frame(frame, c.channels, c.sample_fmt, samples, samples.remaining(), 1))) {
							throw new IllegalStateException(""+averr);
						}
						frame.readField("data");
						frame.data[0].read(0,tmp,0,tmp.length);
						try (SharedPtr<AVPacket> packetPtr = new SharedPtr<AVPacket>(new AVPacket()) {@Override public void close() {avcodec.av_free_packet(ptr);}}) {
							AVPacket packet = packetPtr.ptr;  // one of the few structs from ffmpeg with guaranteed size
							packet.setAutoSynch(false);
							avcodec.av_init_packet(packet);
							packet.size = 0; packet.writeField("size");
							packet.data = null; packet.writeField("data");
							packet.stream_index = st.index; packet.writeField("stream_index");
							avcodec.av_new_packet(packet, samples.remaining());
							
							stuff.add(packet);
							// encode the samples
							IntBuffer gotPacket = IntBuffer.allocate(1);
							System.gc();
							if (0 > (averr = avcodec.avcodec_encode_audio2(c, packet, frame, gotPacket))) {
								throw new IllegalStateException("" + averr);
							} else if (0 != gotPacket.get()) {
								packet.read();
								byte[] arr = new byte[packet.size];
								byte[] arr0 = new byte[samples.remaining()];
								samples.get(arr0);
								packet.data.read(0, arr, 0, packet.size);
//									assert Arrays.equals(arr, arr0);
								st.nb_frames++; st.writeField("nb_frames");
								averr = avformat.av_interleaved_write_frame(fmtCtx, packet);
								if (averr < 0)
									throw new IllegalStateException("" + averr);
							}
							System.out.format("encoded frame: codec time = %d; pts=%d = av_rescale_q(%d,%d/%d,%d/%d) (%.02fs) contains %d samples (%.02fs); got_packet=%d; packet.size=%d%n",
									frame_audio_time,
									frame.pts,
									frame_audio_time, st.codec.time_base.num,st.codec.time_base.den,st.time_base.num,st.time_base.den,
									1.*frame_audio_time/c.sample_rate, frame.nb_samples, 1.*frame.nb_samples/c.sample_rate, gotPacket.array()[0], packet.size);
						}
					}
				}
				if (0 != (err = avformat.av_write_trailer(fmtCtx))) {
					throw new IllegalStateException();
				}
				avformat.avio_flush(ioCtx);
			}
		}
		System.out.println("Done writing");
	}
}