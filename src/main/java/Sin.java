import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Objects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;


import com.sun.jna.Pointer;

/**
 * I'm using Ubuntu 12.04, so probably my library is not the real ffmpeg
 * but libav instead. I might switch later.
 * 
 * <p>
 * I started with this JNAerator command, and then I did the following modifications<br>
 * java -jar ~/Downloads/jnaerator-0.12-SNAPSHOT-20130727.jar  -o wrap-ffmpeg -v -noJar -noComp -package wrapffmpeg -runtime JNA  -library avutil /usr/include/libavutil/avutil.h /usr/include/libavutil/mathematics.h -library avcodec /usr/include/libavcodec/avcodec.h -library avformat /usr/include/libavformat/avformat.h
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
public class Sin {
	public static final long AV_NOPTS_VALUE = 0x8000000000000000l;
	
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

	/**
	 * @param args
	 * @throws IOException 
	 * @throws LineUnavailableException 
	 */
	public static void main(String[] args) throws IOException, LineUnavailableException {
		final AvcodecLibrary avcodec = AvcodecLibrary.INSTANCE;
		final AvformatLibrary avformat = AvformatLibrary.INSTANCE;
		final AvutilLibrary avutil = AvutilLibrary.INSTANCE;
		avcodec.avcodec_register_all();
		avformat.av_register_all();
		AVOutputFormat.ByReference format = null;
		String format_name = "wav", file_url = "file:sinjava.wav";
		for (AVOutputFormat.ByReference formatIter = avformat.av_oformat_next(null); formatIter != null; formatIter = avformat.av_oformat_next(formatIter)) {
			boolean hasEncoder = null != avcodec.avcodec_find_encoder(formatIter.audio_codec);
			if (hasEncoder && false)
				System.out.format("format: audio_codec=% 3d, video_codec=% 5d, hasEncoder=%b, %s %s%n", formatIter.video_codec, formatIter.audio_codec, hasEncoder, formatIter.name, formatIter.long_name);
			formatIter.setAutoWrite(false);
			String iterName = formatIter.name;
			if (format_name.equals(iterName)) {
				format = formatIter;
				break;
			}
		}
		Objects.requireNonNull(format);
		System.out.format("Found format %s%n", format_name);
		AVCodec codec = avcodec.avcodec_find_encoder(format.audio_codec);  // one of AvcodecLibrary.CodecID
		Objects.requireNonNull(codec);
		codec.setAutoWrite(false);
		try (
			SharedPtr<AVFormatContext> fmtCtxPtr = new SharedPtr<AVFormatContext>(avformat.avformat_alloc_context()) {@Override public void close(){if (null!=ptr) avformat.avformat_free_context(ptr);}};
			) {
			AVFormatContext fmtCtx = Objects.requireNonNull(fmtCtxPtr.ptr);
			fmtCtx.setAutoWrite(false);
			fmtCtx.setAutoRead(false);
			fmtCtx.oformat = format; fmtCtx.writeField("oformat");

			AVStream st = avformat.avformat_new_stream(fmtCtx, codec);
			if (null == st)
				throw new IllegalStateException();
			AVCodecContext c = st.codec;
//			AVCodecContext encCtx = avcodec.avcodec_alloc_context();
			if (null == c)
				throw new IllegalStateException();
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
			if ((err = avcodec.avcodec_open(c, codec)) < 0) {
				throw new IllegalStateException();
			}
			try (
//				SharedPtr<AVCodecContext> encCtxPtr = new SharedPtr<AVCodecContext>(c) {@Override public void close() {avcodec.avcodec_close(ptr);}}
				SharedPtr<AVCodecContext> encCtxPtr = new SharedPtr<AVCodecContext>(c) {@Override public void close() {}}
				) {
				assert c.channels != 0;
//				buffer_size = avutil.av_samples_get_buffer_size(null, encCtx.channels, encCtx., sample_fmt, align)
				
//				File f = File.createTempFile("sin", "." + formatName);
				File f = new File("/tmp/sinjava.ogg");
//				f.deleteOnExit();
//				System.out.println("writing to " + f);

				AvformatLibrary.avio_alloc_context_read_packet_callback r = new AvformatLibrary.avio_alloc_context_read_packet_callback() {
					@Override public int apply(Pointer opaque, Pointer buf, int buf_size) {
						System.out.format("Read %d%n", buf_size);
						return buf_size;
					}
				};
				try (FileOutputStream fos = new FileOutputStream(f)) {
					AvformatLibrary.avio_alloc_context_write_packet_callback w = new AvformatLibrary.avio_alloc_context_write_packet_callback() {
						@Override public int apply(Pointer opaque, Pointer buf, int buf_size) {
							System.out.format("Write %d%n", buf_size);
							ByteBuffer buffer = buf.getByteBuffer(0, buf_size);
							try {
								byte[] dst = new byte[buffer.remaining()];
								buffer.get(dst);
								fos.write(dst);
								assert 0 == buffer.remaining();
							} catch (IOException e) {
								// TODO: how to handle this?
								throw new RuntimeException(e);
							}
							return buf_size;
						}
					};
					AVIOContext.ByReference[] ioCtxReference = new AVIOContext.ByReference[1];
					if (0 != (err = avformat.avio_open(ioCtxReference, file_url, AvformatLibrary.AVIO_FLAG_WRITE))) {
						throw new IllegalStateException("averror " + err);
					}
					try (
//						SharedPtr<AVIOContext.ByReference> ioCtxPtr = new SharedPtr<AVIOContext.ByReference>(avformat.avio_alloc_context(outByteBuffer, outByteBuffer.capacity(), 1, null, r, w, null)) {@Override public void close(){if (null!=ptr) avutil.av_free(ptr.getPointer());}}
						SharedPtr<AVIOContext.ByReference> ioCtxPtr = new SharedPtr<AVIOContext.ByReference>(ioCtxReference[0]) {@Override public void close(){if (null!=ptr) avutil.av_free(ptr.getPointer());}}
						) {
	//				fmtCtx.oformat.audio_codec = codec.id;
						AVIOContext.ByReference ioCtx = Objects.requireNonNull(ioCtxPtr.ptr);
						fmtCtx.pb = ioCtx; fmtCtx.writeField("pb");
						int averr = avformat.avformat_write_header(fmtCtx, null);
						if (averr < 0) {
							throw new IllegalStateException("" + averr);
						}
						st.read();  // it is modified by avformat_write_header
						System.out.format("Wrote header. fmtCtx->nb_streams=%d, st->time_base=%d/%d; st->avg_frame_rate=%d/%d%n", fmtCtx.nb_streams, st.time_base.num, st.time_base.den, st.avg_frame_rate.num, st.avg_frame_rate.den); 
						avformat.avio_flush(ioCtx);
						// encode a single tone sound
						int frame_size = c.frame_size != 0 ? c.frame_size : 4096;
						int expectedBufferSize = frame_size * c.channels * (Short.SIZE/8);
						boolean supports_small_last_frame = c.frame_size == 0 ? true : 0 != (codec.capabilities & AvcodecLibrary.CODEC_CAP_SMALL_LAST_FRAME);
						int bufferSize = avutil.av_samples_get_buffer_size((IntBuffer)null, c.channels, frame_size, c.sample_fmt, 1);
						assert bufferSize == expectedBufferSize: String.format("expected %d; got %d", expectedBufferSize, bufferSize);
						ByteBuffer samples = ByteBuffer.allocate(expectedBufferSize);
						samples.order(ByteOrder.nativeOrder());
						try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(javaDataLineInfo)) {
							line.open(javaSoundFormat);
							line.start();
							int audio_time = 0;  // unit: (c.time_base) s = (1/c.sample_rate) s
							int audio_sample_count = supports_small_last_frame ?
								3 * c.sample_rate :
								3 * c.sample_rate / frame_size * frame_size;
							while (audio_time < audio_sample_count) {
								int frame_audio_time = audio_time;
								samples.clear();
								int nb_samples_in_frame = 0;
								if (false) {
									int readCount = line.read(samples.array(), 0, samples.capacity());
									samples.limit(readCount);
									nb_samples_in_frame = readCount / (Short.SIZE/8) / 2;
								} else {
									// TODO: if c.frame_size is 0, use an arbitrary frame size
									for (; samples.hasRemaining() && audio_time < audio_sample_count; nb_samples_in_frame++, audio_time++) {
										double x = 2*Math.PI*440/c.sample_rate * audio_time;
										double y = 10000 * Math.sin(x);
										samples.putShort((short) y);
										samples.putShort((short) y);
									}
									samples.flip();
								}
								try (
										SharedPtr<AVFrame> framePtr = new SharedPtr<AVFrame>(avcodec.avcodec_alloc_frame()) {@Override public void close() {if (null!=ptr) avutil.av_free(ptr.getPointer());}};
										) {
									AVFrame frame = Objects.requireNonNull(framePtr.ptr);
									frame.setAutoRead(false);  // will be an in param
									frame.setAutoWrite(false);
									frame.nb_samples = nb_samples_in_frame; frame.writeField("nb_samples"); // actually unused during encoding
//									frame.format = c.sample_fmt; frame.writeField("format");  // unused during encoding
									
									// Presentation time, in AVStream.time_base units.
									frame.pts = avutil.av_rescale_q(frame_audio_time, c.time_base, st.time_base);  // i * codec_time_base / st_time_base
//								frame.pts = AV_NOPTS_VALUE;
									frame.writeField("pts");
									
//								assert frame.nb_samples > 0;
									assert c.channels > 0;
									int bytesPerSample = avutil.av_get_bytes_per_sample(c.sample_fmt);
									assert bytesPerSample > 0;
//								frame.writeField("nb_samples");
//							int sample_size = avcodec.av_get_bits_per_sample(encCtx.sample_fmt);
//							assert sample_size > 0;
//									System.out.format("avcodec_fill_audio_frame(channels=%d, fmt=%d, contains %d samples; align=%d, pts=rescale(%d,%d/%d,%d/%d)=%d vs %f)%n", c.channels, c.sample_fmt, nb_samples_in_frame, 1,frame_audio_time, st.codec.time_base.num,st.codec.time_base.den,st.time_base.num,st.time_base.den,frame.pts, frame_audio_time*(1.*st.codec.time_base.num/st.codec.time_base.den)/(1.*st.time_base.num/st.time_base.den));
									if (0 != (err = avcodec.avcodec_fill_audio_frame(frame, c.channels, c.sample_fmt, samples, samples.capacity(), 1))) {
										throw new IllegalStateException(""+err);
									}
									AVPacket packet = new AVPacket();  // one of the few structs from ffmpeg with guaranteed size
//									packet.setAutoWrite(false);  // will be an out param
									avcodec.av_init_packet(packet);
									packet.size = 0;
									packet.data = null;
									packet.stream_index = st.index; packet.writeField("stream_index");
									// encode the samples
									IntBuffer gotPacket = IntBuffer.allocate(1);
									if (0 != (err = avcodec.avcodec_encode_audio2(c, packet, frame, gotPacket))) {
										throw new IllegalStateException("" + err);
									} else if (0 != gotPacket.get()) {
//										System.out.format("packet.size=%d; duration=%d%n", packet.readField("size"), packet.readField("duration"));
										packet.read();
//										System.out.format("gotPacket: stream=%d, size=%d, st_index=%s%n", st.index, packet.size, packet.stream_index);
//									ret = avformat.av_interleaved_write_frame(fmtCtx, packet);
										averr = avformat.av_write_frame(fmtCtx, packet);
										if (averr < 0)
											throw new IllegalStateException("" + averr);
//									packet.data.read(0, outbuf, 0, packet.size);
//									fos.write(outbuf, 0, packet.size);
									}
									System.out.format("encoded frame: codec time = %d; pts=%d = av_rescale_q(%d,%d/%d,%d/%d) (%.02fs) contains %d samples (%.02fs); got_packet=%d; packet.size=%d%n",
											frame_audio_time,
											frame.pts,
											frame_audio_time, st.codec.time_base.num,st.codec.time_base.den,st.time_base.num,st.time_base.den,
											1.*frame_audio_time/c.sample_rate, frame.nb_samples, 1.*frame.nb_samples/c.sample_rate, gotPacket.array()[0], packet.size);
//							avcodec.avcodec_encode_audio(encCtx, outByteBuffer, outByteBuffer.remaining(), samples);
								}
							}
						}
						if (0 != (err = avformat.av_write_trailer(fmtCtx))) {
							throw new IllegalStateException();
						}
						avformat.avio_flush(ioCtx);
					}
				}
			}
		}
		System.out.println("Done writing");
	}
}