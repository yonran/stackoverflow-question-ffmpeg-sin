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


public class Sin {
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
				int frame_size = c.frame_size != 0 ? c.frame_size : 4096;
				int expectedBufferSize = frame_size * c.channels * (Short.SIZE/8);
				boolean supports_small_last_frame = c.frame_size == 0 ? true : 0 != (codec.capabilities & AvcodecLibrary.CODEC_CAP_SMALL_LAST_FRAME);
				int bufferSize = avutil.av_samples_get_buffer_size((IntBuffer)null, c.channels, frame_size, c.sample_fmt, 1);
				assert bufferSize == expectedBufferSize: String.format("expected %d; got %d", expectedBufferSize, bufferSize);
				ByteBuffer samples = ByteBuffer.allocate(expectedBufferSize);
				samples.order(ByteOrder.nativeOrder());
				int audio_time = 0;  // unit: (c.time_base) s = (1/c.sample_rate) s
				int audio_sample_count = supports_small_last_frame ?
					3 * c.sample_rate :
					3 * c.sample_rate / frame_size * frame_size;
				while (audio_time < audio_sample_count) {
					int frame_audio_time = audio_time;
					samples.clear();
					int nb_samples_in_frame = 0;
					// encode a single tone sound
					for (; samples.hasRemaining() && audio_time < audio_sample_count; nb_samples_in_frame++, audio_time++) {
						double x = 2*Math.PI*440/c.sample_rate * audio_time;
						double y = 10000 * Math.sin(x);
						samples.putShort((short) y);
						samples.putShort((short) y);
					}
					samples.flip();
					try (
							SharedPtr<AVFrame> framePtr = new SharedPtr<AVFrame>(avcodec.avcodec_alloc_frame()) {@Override public void close() {if (null!=ptr) avutil.av_free(ptr.getPointer());}};
							) {
						AVFrame frame = Objects.requireNonNull(framePtr.ptr);
						frame.setAutoRead(false);  // will be an in param
						frame.setAutoWrite(false);
						frame.nb_samples = nb_samples_in_frame; frame.writeField("nb_samples"); // actually unused during encoding
						// Presentation time, in AVStream.time_base units.
						frame.pts = avutil.av_rescale_q(frame_audio_time, c.time_base, st.time_base);  // i * codec_time_base / st_time_base
						frame.writeField("pts");

						assert c.channels > 0;
						int bytesPerSample = avutil.av_get_bytes_per_sample(c.sample_fmt);
						assert bytesPerSample > 0;
						if (0 != (err = avcodec.avcodec_fill_audio_frame(frame, c.channels, c.sample_fmt, samples, samples.capacity(), 1))) {
							throw new IllegalStateException(""+err);
						}
						AVPacket packet = new AVPacket();  // one of the few structs from ffmpeg with guaranteed size
						avcodec.av_init_packet(packet);
						packet.size = 0;
						packet.data = null;
						packet.stream_index = st.index; packet.writeField("stream_index");
						// encode the samples
						IntBuffer gotPacket = IntBuffer.allocate(1);
						if (0 != (err = avcodec.avcodec_encode_audio2(c, packet, frame, gotPacket))) {
							throw new IllegalStateException("" + err);
						} else if (0 != gotPacket.get()) {
							packet.read();
							averr = avformat.av_write_frame(fmtCtx, packet);
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
				if (0 != (err = avformat.av_write_trailer(fmtCtx))) {
					throw new IllegalStateException();
				}
				avformat.avio_flush(ioCtx);
			}
		}
		System.out.println("Done writing");
	}
}